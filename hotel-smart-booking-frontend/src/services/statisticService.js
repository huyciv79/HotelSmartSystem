import axiosInstance from './axiosInstance';

/**
 * Fetch dashboard statistics for staff (receptionists/managers).
 * GET /statistics
 * @returns {Promise<object>} API response containing dashboard stats
 */
export const getDashboardStats = async () => {
  const response = await axiosInstance.get('/statistics');
  return response.data;
};
