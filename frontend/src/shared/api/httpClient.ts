const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;

  constructor(status: number, code: string | undefined, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
  };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    // Session-cookie auth requires the cookie to be sent on every cross-origin request.
    credentials: 'include',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    let code: string | undefined;
    let message = response.statusText;
    try {
      const errorBody = (await response.json()) as Record<string, unknown>;
      if (typeof errorBody.message === 'string') message = errorBody.message;
      if (typeof errorBody.code === 'string') code = errorBody.code;
    } catch {
      // fall back to statusText already assigned above
    }
    throw new ApiError(response.status, code, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function requestFormData<T>(path: string, formData: FormData): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'PATCH',
    headers: { Accept: 'application/json' },
    credentials: 'include',
    body: formData,
  });

  if (!response.ok) {
    let code: string | undefined;
    let message = response.statusText;
    try {
      const errorBody = (await response.json()) as Record<string, unknown>;
      if (typeof errorBody.message === 'string') message = errorBody.message;
      if (typeof errorBody.code === 'string') code = errorBody.code;
    } catch {
      // fall back to statusText already assigned above
    }
    throw new ApiError(response.status, code, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const httpClient = {
  get<T>(path: string): Promise<T> {
    return request<T>('GET', path);
  },
  post<T>(path: string, body?: unknown): Promise<T> {
    return request<T>('POST', path, body);
  },
  patch<T>(path: string, body?: unknown): Promise<T> {
    return request<T>('PATCH', path, body);
  },
  patchFormData<T>(path: string, formData: FormData): Promise<T> {
    return requestFormData<T>(path, formData);
  },
  put<T>(path: string, body?: unknown): Promise<T> {
    return request<T>('PUT', path, body);
  },
  del<T>(path: string): Promise<T> {
    return request<T>('DELETE', path);
  },
};
