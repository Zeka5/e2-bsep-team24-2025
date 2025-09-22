import React from 'react';
import { useAuth } from '../contexts/AuthContext';

const Dashboard: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-gray-900">
      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="border-4 border-dashed border-gray-700 rounded-lg p-8">
            <div className="text-center">
              <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-full bg-green-600 mb-4">
                <svg className="h-8 w-8 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                </svg>
              </div>
              <h1 className="text-3xl font-bold text-white mb-2">
                Welcome to Dashboard
              </h1>
              <p className="text-xl text-gray-400 mb-6">
                Hello, {user?.name} {user?.surname}!
              </p>

              <div className="bg-gray-800 rounded-lg p-6 text-left">
                <h2 className="text-lg font-semibold text-white mb-4">Account Information</h2>
                <div className="space-y-2">
                  <p className="text-gray-300">
                    <span className="font-medium">Email:</span> {user?.email}
                  </p>
                  {user?.createdAt && (
                    <p className="text-gray-300">
                      <span className="font-medium">Member since:</span> {new Date(user.createdAt).toLocaleDateString()}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;