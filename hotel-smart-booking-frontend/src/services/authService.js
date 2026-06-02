import axiosInstance from './axiosInstance';

/**
 * Register a new user account.
 *
 * POST /auth/register
 * @param {{ fullName: string, email: string, phone: string, password: string }} data
 * @returns {Promise<object>} API response data
 * @throws {import('axios').AxiosError}
 *   - 409: email already exists
 */
export const registerUser = async (data) => {
  const response = await axiosInstance.post('/auth/register', {
    fullName: data.fullName,
    email: data.email,
    phone: data.phone,
    password: data.password,
  });
  return response.data;
};

/**
 * Verify the OTP sent to user's phone/email.
 *
 * POST /auth/verify-otp
 * @param {{ email: string, otp: string }} data
 * @returns {Promise<object>} API response data
 * @throws {import('axios').AxiosError}
 *   - 400: wrong or expired OTP
 */
export const verifyOTP = async ({ email, otp }) => {
  const response = await axiosInstance.post('/auth/verify-otp', { email, otp });
  return response.data;
};

/**
 * Log in a user.
 *
 * POST /auth/login
 * @param {{ email: string, password: string }} data
 * @returns {Promise<object>} API response data
 * @throws {import('axios').AxiosError}
 *   - 401: Unauthorized (incorrect credentials)
 *   - 429: Too Many Requests (rate limited)
 */
export const loginUser = async (data) => {
  const response = await axiosInstance.post('/auth/login', {
    email: data.email,
    password: data.password,
  });
  return response.data;
};

/**
 * Request OTP for forgot password.
 *
 * POST /auth/forgot-password
 * @param {{ email: string }} data
 * @returns {Promise<object>} API response data
 */
export const forgotPassword = async (data) => {
  const response = await axiosInstance.post('/auth/forgot-password', {
    email: data.email,
  });
  return response.data;
};

/**
 * Verify forgot password OTP to receive reset token.
 *
 * POST /auth/verify-forgot-otp
 * @param {{ email: string, otp: string }} data
 * @returns {Promise<object>} API response data with resetToken in data
 */
export const verifyForgotOTP = async ({ email, otp }) => {
  const response = await axiosInstance.post('/auth/verify-forgot-otp', { email, otp });
  return response.data;
};

/**
 * Reset password using the reset token.
 *
 * POST /auth/reset-password
 * @param {{ resetToken: string, newPassword: string }} data
 * @returns {Promise<object>} API response data
 */
export const resetPassword = async ({ resetToken, newPassword }) => {
  const response = await axiosInstance.post('/auth/reset-password', {
    resetToken,
    newPassword,
  });
  return response.data;
};


