import { createBrowserRouter, Outlet } from 'react-router-dom';
import { AppShell } from './layouts/AppShell';
import { AuthLayout } from './layouts/AuthLayout';
import DashboardPage from '../pages/DashboardPage';
import ProjectBoardPage from '../pages/ProjectBoardPage';
import ProjectSettingsPage from '../pages/ProjectSettingsPage';
import ProjectTimelinePage from '../pages/ProjectTimelinePage';
import SignInPage from '../pages/SignInPage';

function AuthLayoutRoute() {
  return (
    <AuthLayout>
      <Outlet />
    </AuthLayout>
  );
}

function AppShellRoute() {
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  );
}

export const router = createBrowserRouter([
  {
    element: <AuthLayoutRoute />,
    children: [{ path: '/sign-in', element: <SignInPage /> }],
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
