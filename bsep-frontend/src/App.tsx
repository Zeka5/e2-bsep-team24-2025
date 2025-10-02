import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import CertificateManagement from './pages/CertificateManagement';
import UserManagement from './pages/UserManagement';
import CAAssignmentManagement from './pages/CAAssignmentManagement';
import MyCSRs from './pages/MyCSRs';
import CSRReview from './pages/CSRReview';
import MyCertificates from './pages/MyCertificates';
import { ROUTES } from './constants/routes';
import { ROLES } from './constants/roles';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="App">
          <Navbar />
          <Routes>
            {/* Public routes */}
            <Route path={ROUTES.LOGIN} element={<Login />} />
            <Route path={ROUTES.REGISTER} element={<Register />} />

            {/* Protected routes */}
            <Route
              path={ROUTES.DASHBOARD}
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />

            {/* Admin only routes */}
            <Route
              path={ROUTES.ADMIN}
              element={
                <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
                  <div className="min-h-screen bg-gray-900 flex items-center justify-center">
                    <div className="text-white text-2xl">Admin Panel - Coming Soon</div>
                  </div>
                </ProtectedRoute>
              }
            />
            <Route
              path={ROUTES.CERTIFICATES}
              element={
                <ProtectedRoute allowedRoles={[ROLES.ADMIN, ROLES.CA]}>
                  <CertificateManagement />
                </ProtectedRoute>
              }
            />
            <Route
              path={ROUTES.USERS}
              element={
                <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
                  <UserManagement />
                </ProtectedRoute>
              }
            />
            <Route
              path={ROUTES.CA_ASSIGNMENTS}
              element={
                <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
                  <CAAssignmentManagement />
                </ProtectedRoute>
              }
            />
            <Route
              path={ROUTES.CSR_REVIEW}
              element={
                <ProtectedRoute allowedRoles={[ROLES.ADMIN, ROLES.CA]}>
                  <CSRReview />
                </ProtectedRoute>
              }
            />

            {/* User routes */}
            <Route
              path={ROUTES.MY_CSRS}
              element={
                <ProtectedRoute>
                  <MyCSRs />
                </ProtectedRoute>
              }
            />
            <Route
              path={ROUTES.MY_CERTIFICATES}
              element={
                <ProtectedRoute>
                  <MyCertificates />
                </ProtectedRoute>
              }
            />

            {/* Default redirect */}
            <Route path={ROUTES.HOME} element={<Navigate to={ROUTES.DASHBOARD} replace />} />
          </Routes>
        </div>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
