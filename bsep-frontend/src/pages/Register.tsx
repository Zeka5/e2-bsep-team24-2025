import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useApiHandler } from '../utils/handleApi';
import { UserRole } from '../types/user';
import { ROUTES } from '../constants/routes';
import { validatePassword, getPasswordStrengthLabel, getPasswordStrengthColor, PasswordStrength } from '../utils/passwordValidation';

const Register: React.FC = () => {
  const [formData, setFormData] = useState({
    name: '',
    surname: '',
    email: '',
    password: '',
    confirmPassword: '',
    organization: '',
    role: UserRole.USER
  });
  const [passwordMismatch, setPasswordMismatch] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState<PasswordStrength | null>(null);
  const { register } = useAuth();
  const { loading, error, success, clearMessages } = useApiHandler();
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    const updatedFormData = {
      ...formData,
      [name]: value
    };
    setFormData(updatedFormData);

    // Check password strength dynamically
    if (name === 'password') {
      const strength = validatePassword(value);
      setPasswordStrength(strength);
    }

    // Check password match dynamically
    if (name === 'password' || name === 'confirmPassword') {
      const password = name === 'password' ? value : updatedFormData.password;
      const confirmPassword = name === 'confirmPassword' ? value : updatedFormData.confirmPassword;
      setPasswordMismatch(confirmPassword !== '' && password !== confirmPassword);
    }
  };

  // Check if form is valid for submission
  const isFormValid = () => {
    return formData.password.length >= 15 &&
           formData.password.length <= 65 &&
           formData.password === formData.confirmPassword &&
           formData.confirmPassword !== '';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearMessages();

    try {
      await register({
        name: formData.name,
        surname: formData.surname,
        email: formData.email,
        password: formData.password,
        organization: formData.organization,
        role: formData.role
      });

      setTimeout(() => {
        navigate(ROUTES.LOGIN);
      }, 2000);
    } catch (err) {
      // Error handling is done by useApiHandler
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-full bg-red-600">
            <svg className="h-8 w-8 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z" />
            </svg>
          </div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-white">
            Create Account
          </h2>
          <p className="mt-2 text-center text-sm text-gray-400">
            Join the secure platform
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-300">
                  First Name
                </label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  required
                  className="mt-1 appearance-none relative block w-full px-3 py-2 bg-gray-800 border border-gray-700 placeholder-gray-500 text-white rounded-md focus:outline-none focus:ring-red-500 focus:border-red-500 sm:text-sm"
                  placeholder="First name"
                  value={formData.name}
                  onChange={handleChange}
                />
              </div>
              <div>
                <label htmlFor="surname" className="block text-sm font-medium text-gray-300">
                  Last Name
                </label>
                <input
                  id="surname"
                  name="surname"
                  type="text"
                  required
                  className="mt-1 appearance-none relative block w-full px-3 py-2 bg-gray-800 border border-gray-700 placeholder-gray-500 text-white rounded-md focus:outline-none focus:ring-red-500 focus:border-red-500 sm:text-sm"
                  placeholder="Last name"
                  value={formData.surname}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-300">
                Email Address
              </label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-2 bg-gray-800 border border-gray-700 placeholder-gray-500 text-white rounded-md focus:outline-none focus:ring-red-500 focus:border-red-500 sm:text-sm"
                placeholder="Email address"
                value={formData.email}
                onChange={handleChange}
              />
            </div>

            <div>
              <label htmlFor="organization" className="block text-sm font-medium text-gray-300">
                Organization
              </label>
              <input
                id="organization"
                name="organization"
                type="text"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-2 bg-gray-800 border border-gray-700 placeholder-gray-500 text-white rounded-md focus:outline-none focus:ring-red-500 focus:border-red-500 sm:text-sm"
                placeholder="Organization name"
                value={formData.organization}
                onChange={handleChange}
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-300">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="new-password"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-2 bg-gray-800 border border-gray-700 placeholder-gray-500 text-white rounded-md focus:outline-none focus:ring-red-500 focus:border-red-500 sm:text-sm"
                placeholder="Password (15-65 characters)"
                value={formData.password}
                onChange={handleChange}
              />

              {/* Password Strength Meter */}
              {passwordStrength && formData.password && (
                <div className="mt-2 space-y-2">
                  {/* Strength Bar */}
                  <div className="flex items-center gap-2">
                    <div className="flex-1 bg-gray-700 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full transition-all duration-300 ${getPasswordStrengthColor(passwordStrength.score)}`}
                        style={{ width: `${(passwordStrength.score / 6) * 100}%` }}
                      />
                    </div>
                    <span className="text-xs text-gray-300 min-w-[80px]">
                      {getPasswordStrengthLabel(passwordStrength.score)}
                    </span>
                  </div>

                  {/* Requirements Checklist */}
                  <div className="grid grid-cols-2 gap-1 text-xs">
                    <div className={passwordStrength.hasMinLength ? 'text-green-400' : 'text-red-400'}>
                      {passwordStrength.hasMinLength ? '✓' : '✗'} Min 15 chars
                    </div>
                    <div className={passwordStrength.hasMaxLength ? 'text-green-400' : 'text-red-400'}>
                      {passwordStrength.hasMaxLength ? '✓' : '✗'} Max 65 chars
                    </div>
                    <div className={passwordStrength.hasUppercase ? 'text-green-400' : 'text-red-400'}>
                      {passwordStrength.hasUppercase ? '✓' : '✗'} Uppercase
                    </div>
                    <div className={passwordStrength.hasLowercase ? 'text-green-400' : 'text-red-400'}>
                      {passwordStrength.hasLowercase ? '✓' : '✗'} Lowercase
                    </div>
                    <div className={passwordStrength.hasNumber ? 'text-green-400' : 'text-red-400'}>
                      {passwordStrength.hasNumber ? '✓' : '✗'} Number
                    </div>
                    <div className={passwordStrength.hasSpecialChar ? 'text-green-400' : 'text-red-400'}>
                      {passwordStrength.hasSpecialChar ? '✓' : '✗'} Special char
                    </div>
                  </div>

                  {/* Common Password Warning */}
                  {!passwordStrength.isNotCommon && (
                    <p className="text-xs text-red-400 font-semibold">
                      ⚠ This is a common password!
                    </p>
                  )}
                </div>
              )}
            </div>

            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-300">
                Confirm Password
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                required
                className={`mt-1 appearance-none relative block w-full px-3 py-2 bg-gray-800 border ${passwordMismatch ? 'border-red-500' : 'border-gray-700'} placeholder-gray-500 text-white rounded-md focus:outline-none focus:ring-red-500 focus:border-red-500 sm:text-sm`}
                placeholder="Confirm password"
                value={formData.confirmPassword}
                onChange={handleChange}
              />
              {passwordMismatch && (
                <p className="mt-1 text-sm text-red-400">Passwords do not match</p>
              )}
            </div>
          </div>

          {error && (
            <div className="bg-red-900 border border-red-700 text-red-100 px-4 py-3 rounded">
              {error}
            </div>
          )}

          {success && (
            <div className="bg-green-900 border border-green-700 text-green-100 px-4 py-3 rounded">
              {success}
            </div>
          )}

          <div>
            <button
              type="submit"
              disabled={loading || !isFormValid()}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span className="absolute left-0 inset-y-0 flex items-center pl-3">
                <svg className="h-5 w-5 text-red-500 group-hover:text-red-400" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z" />
                </svg>
              </span>
              {loading ? 'Creating Account...' : 'Create Account'}
            </button>
          </div>

          <div className="text-center">
            <p className="text-sm text-gray-400">
              Already have an account?{' '}
              <Link to={ROUTES.LOGIN} className="font-medium text-red-400 hover:text-red-300">
                Sign in here
              </Link>
            </p>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Register;