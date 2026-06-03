import axiosInstance from './axiosInstance';

/**
 * Fetch the current user's profile details.
 * GET /users/profile
 * @returns {Promise<object>} API response with user profile details
 */
export const getUserProfile = async () => {
  const response = await axiosInstance.get('/users/profile');
  return response.data;
};

/**
 * Update the user's profile information.
 * PUT /users/profile
 * @param {{ fullName: string, address: string }} data
 * @returns {Promise<object>} API response with updated user profile details
 */
export const updateUserProfile = async (data) => {
  const response = await axiosInstance.put('/users/profile', {
    fullName: data.fullName,
    address: data.address,
  });
  return response.data;
};

/**
 * Upload the user's avatar.
 * POST /users/avatar
 * @param {File} file
 * @returns {Promise<object>} API response containing the new avatar URL
 */
export const uploadAvatar = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axiosInstance.post('/users/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};
