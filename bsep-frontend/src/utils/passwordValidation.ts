import { isCommonPassword } from './commonPasswords';

export interface PasswordStrength {
  score: number; // 0-5
  hasMinLength: boolean;
  hasMaxLength: boolean;
  hasUppercase: boolean;
  hasLowercase: boolean;
  hasNumber: boolean;
  hasSpecialChar: boolean;
  isNotCommon: boolean;
  feedback: string[];
}

export const validatePassword = (password: string): PasswordStrength => {
  const feedback: string[] = [];

  const hasMinLength = password.length >= 15;
  const hasMaxLength = password.length <= 65;
  const hasUppercase = /[A-Z]/.test(password);
  const hasLowercase = /[a-z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
  const isNotCommon = !isCommonPassword(password);

  // Calculate score (0-5)
  let score = 0;
  if (hasMinLength && hasMaxLength) score++;
  if (hasUppercase) score++;
  if (hasLowercase) score++;
  if (hasNumber) score++;
  if (hasSpecialChar) score++;
  if (isNotCommon) score++;

  // If length requirements not met, cap score at 0
  if (!hasMinLength || !hasMaxLength) {
    score = 0;
  }

  // Generate feedback
  if (!hasMinLength) feedback.push('Password must be at least 15 characters');
  if (!hasMaxLength) feedback.push('Password must not exceed 65 characters');
  if (!hasUppercase) feedback.push('Add uppercase letters');
  if (!hasLowercase) feedback.push('Add lowercase letters');
  if (!hasNumber) feedback.push('Add numbers');
  if (!hasSpecialChar) feedback.push('Add special characters');
  if (!isNotCommon) feedback.push('This is a common password, choose a different one');

  return {
    score,
    hasMinLength,
    hasMaxLength,
    hasUppercase,
    hasLowercase,
    hasNumber,
    hasSpecialChar,
    isNotCommon,
    feedback
  };
};

export const getPasswordStrengthLabel = (score: number): string => {
  if (score === 0) return 'Too Weak';
  if (score === 1) return 'Very Weak';
  if (score === 2) return 'Weak';
  if (score === 3) return 'Fair';
  if (score === 4) return 'Good';
  if (score === 5) return 'Strong';
  if (score === 6) return 'Very Strong';
  return 'Too Weak';
};

export const getPasswordStrengthColor = (score: number): string => {
  if (score === 0) return 'bg-red-600';
  if (score === 1) return 'bg-red-500';
  if (score === 2) return 'bg-orange-500';
  if (score === 3) return 'bg-yellow-500';
  if (score === 4) return 'bg-lime-500';
  if (score === 5) return 'bg-green-500';
  if (score === 6) return 'bg-green-600';
  return 'bg-red-600';
};
