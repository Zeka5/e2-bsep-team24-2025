import React, { useState, useEffect } from 'react';
import { UserDto } from '../types/user';
import { CreateCAUserRequest } from '../types/caUser';
import { userService } from '../services/userService';

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showCreateCAUserForm, setShowCreateCAUserForm] = useState(false);

  const [caUserForm, setCAUserForm] = useState<CreateCAUserRequest>({
    name: '',
    surname: '',
    email: '',
    organization: ''
  });

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const allUsers = await userService.getAllUsers();
      setUsers(allUsers);
    } catch (err: any) {
      setError('Failed to load users');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCAUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    try {
      await userService.createCAUser(caUserForm);
      setSuccess('CA user created successfully! Temporary password has been sent to their email.');
      setCAUserForm({
        name: '',
        surname: '',
        email: '',
        organization: ''
      });
      setShowCreateCAUserForm(false);
      loadUsers();
    } catch (err: any) {
      setError(err.response?.data || 'Failed to create CA user');
      console.error(err);
    }
  };

  const handleDeleteUser = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete user ${name}?`)) {
      return;
    }

    try {
      await userService.deleteUser(id);
      setSuccess('User deleted successfully');
      loadUsers();
    } catch (err: any) {
      setError(err.response?.data || 'Failed to delete user');
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-white text-xl">Loading users...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-white">User Management</h1>
          <button
            onClick={() => setShowCreateCAUserForm(true)}
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded"
          >
            Create CA User
          </button>
        </div>

        {error && (
          <div className="bg-red-600 text-white p-4 rounded mb-6">
            {error}
            <button onClick={() => setError(null)} className="ml-4 text-red-200 hover:text-white">×</button>
          </div>
        )}

        {success && (
          <div className="bg-green-600 text-white p-4 rounded mb-6">
            {success}
            <button onClick={() => setSuccess(null)} className="ml-4 text-green-200 hover:text-white">×</button>
          </div>
        )}

        {/* CA User Creation Form */}
        {showCreateCAUserForm && (
          <div className="bg-gray-800 p-6 rounded mb-6">
            <h2 className="text-xl font-bold text-white mb-4">Create CA User</h2>
            <form onSubmit={handleCreateCAUser} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-white mb-2">Name</label>
                  <input
                    type="text"
                    value={caUserForm.name}
                    onChange={(e) => setCAUserForm({ ...caUserForm, name: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    placeholder="John"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white mb-2">Surname</label>
                  <input
                    type="text"
                    value={caUserForm.surname}
                    onChange={(e) => setCAUserForm({ ...caUserForm, surname: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    placeholder="Doe"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white mb-2">Email</label>
                  <input
                    type="email"
                    value={caUserForm.email}
                    onChange={(e) => setCAUserForm({ ...caUserForm, email: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    placeholder="john.doe@example.com"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white mb-2">Organization</label>
                  <input
                    type="text"
                    value={caUserForm.organization}
                    onChange={(e) => setCAUserForm({ ...caUserForm, organization: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    placeholder="Example Corp"
                    required
                  />
                </div>
              </div>
              <div className="text-sm text-gray-400">
                A temporary password will be automatically generated and sent to the user's email.
                They will be required to change it on first login.
              </div>
              <div className="flex space-x-4">
                <button type="submit" className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded">
                  Create CA User
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateCAUserForm(false)}
                  className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Users List */}
        <div className="bg-gray-800 rounded overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-700">
              <tr>
                <th className="text-left p-4 text-white">Name</th>
                <th className="text-left p-4 text-white">Email</th>
                <th className="text-left p-4 text-white">Organization</th>
                <th className="text-left p-4 text-white">Role</th>
                <th className="text-left p-4 text-white">Created At</th>
                <th className="text-left p-4 text-white">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} className="border-t border-gray-700">
                  <td className="p-4 text-white">
                    {user.name} {user.surname}
                  </td>
                  <td className="p-4 text-gray-300">{user.email}</td>
                  <td className="p-4 text-gray-300">{user.organization}</td>
                  <td className="p-4 text-white">
                    <span className={`px-2 py-1 rounded text-xs ${
                      user.role === 'ADMIN' ? 'bg-red-600' :
                      user.role === 'CA' ? 'bg-yellow-600' : 'bg-blue-600'
                    }`}>
                      {user.role}
                    </span>
                  </td>
                  <td className="p-4 text-gray-300">
                    {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
                  </td>
                  <td className="p-4">
                    {user.role !== 'ADMIN' && (
                      <button
                        onClick={() => handleDeleteUser(user.id!, `${user.name} ${user.surname}`)}
                        className="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-sm"
                      >
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {users.length === 0 && (
            <div className="p-8 text-center text-gray-400">
              No users found.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserManagement;
