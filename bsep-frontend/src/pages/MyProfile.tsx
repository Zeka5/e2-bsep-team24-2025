import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { sessionService } from '../services/sessionService';
import { UserSession } from '../types/session';
import { getSessionIdFromToken } from '../utils/jwtUtils';
import { authService } from '../services/authService';
import { ROUTES } from '../constants/routes';

const MyProfile: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<UserSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);

  useEffect(() => {
    // Get current session ID from token
    const token = authService.getToken();
    if (token) {
      const sessionId = getSessionIdFromToken(token);
      setCurrentSessionId(sessionId);
    }
    loadSessions();
  }, []);

  const loadSessions = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await sessionService.getActiveSessions();
      setSessions(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load sessions');
    } finally {
      setLoading(false);
    }
  };

  const handleRevokeSession = async (sessionId: string) => {
    const isCurrentSession = sessionId === currentSessionId;
    const confirmMessage = isCurrentSession
      ? 'Are you sure you want to revoke this session? You will be logged out immediately.'
      : 'Are you sure you want to revoke this session?';

    if (!window.confirm(confirmMessage)) {
      return;
    }

    try {
      await sessionService.revokeSession(sessionId);

      if (isCurrentSession) {
        // If revoking current session, logout and redirect to login
        await logout();
        navigate(ROUTES.LOGIN);
      } else {
        // Otherwise, just reload the sessions list
        await loadSessions();
      }
    } catch (err: any) {
      // If we get 401, it means our session was revoked, logout
      if (err.response?.status === 401) {
        await logout();
        navigate(ROUTES.LOGIN);
      } else {
        setError(err.response?.data?.message || 'Failed to revoke session');
      }
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getDeviceIcon = (deviceType: string) => {
    switch (deviceType.toLowerCase()) {
      case 'mobile':
        return (
          <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M7 2a2 2 0 00-2 2v12a2 2 0 002 2h6a2 2 0 002-2V4a2 2 0 00-2-2H7zm3 14a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
          </svg>
        );
      case 'tablet':
        return (
          <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm4 14a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
          </svg>
        );
      default:
        return (
          <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v8a2 2 0 01-2 2h-2.22l.123.489.804.804A1 1 0 0113 18H7a1 1 0 01-.707-1.707l.804-.804L7.22 15H5a2 2 0 01-2-2V5zm5.771 7H5V5h10v7H8.771z" clipRule="evenodd" />
          </svg>
        );
    }
  };

  return (
    <div className="min-h-screen bg-gray-900">
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          {/* Profile Header */}
          <div className="bg-gray-800 rounded-lg p-6 mb-6 border border-gray-700">
            <div className="flex items-center space-x-4">
              <div className="h-16 w-16 rounded-full bg-red-600 flex items-center justify-center">
                <span className="text-2xl font-bold text-white">
                  {user?.name?.[0]}{user?.surname?.[0]}
                </span>
              </div>
              <div>
                <h1 className="text-2xl font-bold text-white">
                  {user?.name} {user?.surname}
                </h1>
                <p className="text-gray-400">{user?.email}</p>
                <p className="text-sm text-gray-500 mt-1">
                  <span className="font-medium">Role:</span> {user?.role}
                </p>
              </div>
            </div>
          </div>

          {/* Active Sessions */}
          <div className="bg-gray-800 rounded-lg border border-gray-700">
            <div className="px-6 py-4 border-b border-gray-700">
              <h2 className="text-xl font-bold text-white">Active Sessions</h2>
              <p className="text-sm text-gray-400 mt-1">
                Manage your active login sessions across different devices
              </p>
            </div>

            <div className="p-6">
              {loading ? (
                <div className="text-center py-8">
                  <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
                  <p className="text-gray-400 mt-2">Loading sessions...</p>
                </div>
              ) : error ? (
                <div className="bg-red-900/20 border border-red-700 rounded-lg p-4">
                  <p className="text-red-400">{error}</p>
                </div>
              ) : sessions.length === 0 ? (
                <div className="text-center py-8">
                  <svg className="mx-auto h-12 w-12 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                  <p className="text-gray-400 mt-2">No active sessions found</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {sessions.map((session) => {
                    const isCurrentSession = session.sessionId === currentSessionId;
                    return (
                      <div
                        key={session.sessionId}
                        className={`bg-gray-700/50 rounded-lg p-4 border transition-colors ${
                          isCurrentSession
                            ? 'border-red-600 hover:border-red-500'
                            : 'border-gray-600 hover:border-gray-500'
                        }`}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex items-start space-x-4">
                            <div className="text-gray-400 mt-1">
                              {getDeviceIcon(session.deviceType)}
                            </div>
                            <div className="flex-1">
                              <div className="flex items-center space-x-2">
                                <h3 className="text-lg font-semibold text-white">
                                  {session.deviceType}
                                </h3>
                                <span className="px-2 py-1 text-xs font-medium bg-green-900/30 text-green-400 rounded">
                                  Active
                                </span>
                                {isCurrentSession && (
                                  <span className="px-2 py-1 text-xs font-medium bg-red-900/30 text-red-400 rounded">
                                    Current Session
                                  </span>
                                )}
                              </div>
                            <div className="mt-2 space-y-1">
                              <p className="text-sm text-gray-400">
                                <span className="font-medium text-gray-300">Browser:</span> {session.browser}
                              </p>
                              <p className="text-sm text-gray-400">
                                <span className="font-medium text-gray-300">IP Address:</span> {session.ipAddress}
                              </p>
                              <p className="text-sm text-gray-400">
                                <span className="font-medium text-gray-300">Last Activity:</span> {formatDate(session.lastActivity)}
                              </p>
                              <p className="text-sm text-gray-400">
                                <span className="font-medium text-gray-300">Login Time:</span> {formatDate(session.createdAt)}
                              </p>
                            </div>
                          </div>
                        </div>
                        <button
                          onClick={() => handleRevokeSession(session.sessionId)}
                          className="ml-4 px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-md transition-colors"
                        >
                          Revoke
                        </button>
                      </div>
                    </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MyProfile;
