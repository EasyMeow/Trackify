import { createBrowserRouter, Outlet } from 'react-router-dom';
import { RequireAnonymous, RequireAuth } from './guards/RouteGuards';
import { AppShell } from './layouts/AppShell';
import { AuthLayout } from './layouts/AuthLayout';
import DashboardPage from '../pages/DashboardPage';
import ProjectBoardPage from '../pages/ProjectBoardPage';
import ProjectSettingsPage from '../pages/ProjectSettingsPage';
import ProjectTimelinePage from '../pages/ProjectTimelinePage';
import SignInPage from '../pages/SignInPage';
import SignUpPage from '../pages/SignUpPage';

function AuthLayoutRoute() {
  return (
    <RequireAnonymous>
      <AuthLayout>
        <Outlet />
      </AuthLayout>
    </RequireAnonymous>
  );
}

function AppShellRoute() {
  return (
    <RequireAuth>
      <AppShell>
        <Outlet />
      </AppShell>
    </RequireAuth>
  );
}

export const router = createBrowserRouter([
  {
    element: <AuthLayoutRoute />,
    children: [
      { path: '/sign-in', element: <SignInPage /> },
      { path: '/sign-up', element: <SignUpPage /> },
    ],
  },
  {
    element: <AppShellRoute />,
    children: [
      { path: '/', element: <DashboardPage /> },
      { path: '/projects/:projectId/board', element: <ProjectBoardPage /> },
      { path: '/projects/:projectId/timeline', element: <ProjectTimelinePage /> },
      { path: '/projects/:projectId/settings', element: <ProjectSettingsPage /> },
    ],
  },
]);
