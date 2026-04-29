#!/bin/bash
set -e

TASKS_FILE="tasks.json"

# Запуск предварительных проверок через codex.
# По умолчанию включено. Отключить можно флагом --skip-preflight / -s
# или переменной окружения RALPH_SKIP_PREFLIGHT=1.
RUN_PREFLIGHT=1
if [[ "${RALPH_SKIP_PREFLIGHT:-0}" == "1" ]]; then
    RUN_PREFLIGHT=0
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-preflight|-s)
            RUN_PREFLIGHT=0
            shift
            ;;
        --preflight)
            RUN_PREFLIGHT=1
            shift
            ;;
        -h|--help)
            cat <<USAGE
Использование: $0 [опции]

Опции:
  --skip-preflight, -s   Пропустить предварительные проверки через codex
  --preflight            Принудительно запустить проверки (по умолчанию)
  -h, --help             Показать эту справку

Переменные окружения:
  RALPH_AGENT=claude|codex   Принудительно выбрать агента
  RALPH_SKIP_PREFLIGHT=1     Аналог --skip-preflight
USAGE
            exit 0
            ;;
        *)
            echo "Неизвестная опция: $1" >&2
            echo "Используйте --help для списка опций." >&2
            exit 1
            ;;
    esac
done

# Agent selection:
# - Set RALPH_AGENT=claude or RALPH_AGENT=codex to force.
# - Otherwise auto-detect (prefers Claude if available).
resolve_agent() {
    if [[ -n "${RALPH_AGENT:-}" ]]; then
        echo "$RALPH_AGENT"
        return 0
    fi
    if command -v claude >/dev/null 2>&1; then
        echo "claude"
        return 0
    fi
    if command -v codex >/dev/null 2>&1; then
        echo "codex"
        return 0
    fi
    return 1
}

run_agent() {
    local agent="$1"
    local prompt="$2"

    case "$agent" in
        claude)
            claude --permission-mode acceptEdits -p "$prompt"
            ;;
        codex)
            local output_file
            output_file="$(mktemp -t ralph_codex.XXXXXX)"
            # Use non-interactive Codex exec and capture only the last message.
            codex exec --full-auto --color never -C "$PWD" --output-last-message "$output_file" "$prompt" >/dev/null
            cat "$output_file"
            rm -f "$output_file"
            ;;
        *)
            echo "Unsupported agent: $agent" >&2
            return 1
            ;;
    esac
}

# Предварительные проверки через codex.
# 1) Анализ блокеров окружения (инструменты, права, зависимости).
# 2) Аудит конфигурации Claude (agents, skills, settings).
preflight_codex() {
    if ! command -v codex >/dev/null 2>&1; then
        echo "⚠ codex не найден — пропускаю предварительные проверки." >&2
        return 0
    fi

    echo "==================================="
    echo "Этап 1/2: проверка блокеров через codex"
    echo "==================================="
    local blocker_prompt
    blocker_prompt=$(cat <<'EOF'
Проверь окружение проекта на блокеры перед началом разработки.
Не устанавливай и не настраивай ничего сам — только проанализируй и отчитайся.

Что проверить:
- Какие CLI-инструменты установлены и каких не хватает (git, uv, python, claude, ruff и любые другие, упомянутые в проекте).
- Есть ли проблемы с правами доступа в текущей рабочей директории.
- Установлены ли зависимости проекта (pyproject.toml / package.json и т.п.).
- Доступны ли API-ключи / учётные данные, если они требуются.
- Пока что игнорируй докер, из терминала он доступен
Если есть блокеры — кратко перечисли их и что нужно установить/настроить (с командами).
В САМОМ КОНЦЕ ответа выведи РОВНО ОДНУ из строк:
<status>OK</status>      — если можно начинать работу
<status>BLOCKED</status> — если есть критические блокеры
EOF
)
    local blocker_result
    blocker_result=$(run_agent codex "$blocker_prompt")
    echo "$blocker_result"

    if [[ "$blocker_result" == *"<status>BLOCKED</status>"* ]]; then
        echo ""
        echo "✗ Обнаружены блокеры. Устраните их и перезапустите ralph.sh." >&2
        exit 1
    fi

    echo ""
    echo "==================================="
    echo "Этап 2/2: аудит настроек claude через codex"
    echo "==================================="
    local audit_prompt
    audit_prompt=$(cat <<'EOF'
Проанализируй конфигурацию Claude Code и сообщи об ошибках или нестыковках.
НЕ изменяй файлы — только отчёт.

Что проверить:
- settings.json: ~/.claude/settings.json, ./.claude/settings.json, ./.claude/settings.local.json — корректность JSON, hooks, permissions, env.
- Агенты: ~/.claude/agents/*.md и ./.claude/agents/*.md — валидность frontmatter (name, description, tools), пересекающиеся имена, опечатки в именах инструментов.
- Скиллы: ~/.claude/skills/* и ./.claude/skills/* — наличие SKILL.md, корректность frontmatter и описаний.
- Команды/хуки: видимые проблемы, устаревшие или несовместимые настройки.

Кратко перечисли найденные проблемы пунктами с путями к файлам.
Если всё в порядке — напиши "Настройки Claude в порядке".
EOF
)
    local audit_result
    audit_result=$(run_agent codex "$audit_prompt")
    echo "$audit_result"
    echo ""
}

# Функция проверки наличия pending задач
has_pending_tasks() {
    pending_count=$(grep -c '"status": "pending"' "$TASKS_FILE" 2>/dev/null || echo "0")
    [ "$pending_count" -gt 0 ]
}

if [[ "$RUN_PREFLIGHT" == "1" ]]; then
    preflight_codex
else
    echo "⏭  Предварительные проверки через codex пропущены (--skip-preflight)."
fi

iteration=1

while has_pending_tasks; do
    echo "Итерация $iteration"
    echo "-----------------------------------"

    # Показываем текущий статус задач
    pending=$(grep -c '"status": "pending"' "$TASKS_FILE" 2>/dev/null || echo "0")
    done_count=$(grep -c '"status": "done"' "$TASKS_FILE" 2>/dev/null || echo "0")
    echo "Задач pending: $pending, done: $done_count"
    echo "-----------------------------------"

    agent=$(resolve_agent) || {
        echo "Не найден поддерживаемый агент. Установите 'claude' или 'codex', либо задайте RALPH_AGENT." >&2
        exit 1
    }

    prompt=$(cat <<'EOF'
@tasks.json @progress.txt
1. Найди фичу с наивысшим приоритетом и работай ТОЛЬКО над ней.
Это должна быть фича, которую ТЫ считаешь наиболее приоритетной — не обязательно первая в списке.
2. Если фича затрагивает backend, проверь компиляцию через '(cd backend && ./mvnw -q compile test-compile)' и тесты через '(cd backend && ./mvnw -q test)'. Если backend ещё не инициализирован (нет backend/mvnw или backend/pom.xml), пропусти этот шаг.
3. Обнови TASK с информацией о выполненной работе.
4. Добавь свой прогресс в файл progress.txt.
Используй это, чтобы оставить заметку для следующей итерации работы над кодом.
5. Сделай git commit для этой фичи.
РАБОТАЙ ТОЛЬКО НАД ОДНОЙ ФИЧЕЙ.
Если при реализации фичи ты заметишь, что TASK полностью выполнен, выведи <promise>COMPLETE</promise>.
EOF
)

    result=$(run_agent "$agent" "$prompt")

    echo "$result"

    if [[ "$result" == *"<promise>COMPLETE</promise>"* ]]; then
        echo "✓ TASK выполнен!"
        # Проверяем, остались ли ещё pending задачи
        remaining=$(grep -c '"status": "pending"' "$TASKS_FILE" 2>/dev/null || echo "0")
        if [ "$remaining" -eq 0 ]; then
            echo "🎉 Все задачи выполнены!"
            say -v Milena "Хозяин, я всё сделалъ!"
            exit 0
        fi
        echo "Осталось задач: $remaining. Продолжаю..."
        say -v Milena "Задача готова. Продолжаю работу."
    fi

    ((iteration++))
done

echo "Все задачи выполнены! Итераций: $((iteration-1))"
say -v Milena "Хозяин, я сделалъ!"
