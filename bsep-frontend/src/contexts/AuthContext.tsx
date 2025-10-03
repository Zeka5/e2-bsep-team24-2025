import React, { createContext, useContext, useEffect, useState } from 'react';
import { AuthContextType, UserDto } from '../types/user';
import { authService } from '../services/authService';
import { sessionService } from '../services/sessionService';
import { useApiHandler } from '../utils/handleApi';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<UserDto | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const { handleApi } = useApiHandler();

  // Initialize auth state from localStorage and verify session
  useEffect(() => {
    const initializeAuth = async () => {
      const savedToken = authService.getToken();
      const savedUser = authService.getUser();

      if (savedToken && savedUser) {
        // Verify session is still active by making an API call BEFORE setting state
        try {
          const userService = await import('../services/userService');
          await userService.userService.getMyInfo();

          // Only set token and user if API call succeeds
          setToken(savedToken);
          setUser(savedUser);
        } catch (error: any) {
          // Session is invalid - clear everything immediately
          console.warn('Session expired or revoked, clearing auth data');
          authService.logout();
          setToken(null);
          setUser(null);

          // Force redirect to login
          window.location.href = '/login';
        }
      }

      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (email: string, password: string, captchaToken: string): Promise<void> => {
    const result = await handleApi(
      () => authService.login({ email, password, captchaToken }),
      {
        successMessage: 'Login successful',
        errorMessage: 'Login failed'
      }
    );

    if (result) {
      const { user: userData, token: authToken } = result;

      // Save to localStorage
      authService.setToken(authToken);
      authService.setUser(userData);

      // Update state
      setToken(authToken);
      setUser(userData);
    }
  };

  const register = async (userData: UserDto): Promise<void> => {
    await handleApi(
      () => authService.register(userData),
      {
        successMessage: 'Registration successful! Please check your email for activation.',
        errorMessage: 'Registration failed'
      }
    );
  };

  const logout = async (): Promise<void> => {
    try {
      await sessionService.logout();
    } catch (error) {
      console.error('Error during logout:', error);
    } finally {
      authService.logout();
      setToken(null);
      setUser(null);
    }
  };

  const isAuthenticated = !!token && !!user;

  const value: AuthContextType = {
    user,
    token,
    login,
    register,
    logout,
    isAuthenticated,
    isLoading,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};