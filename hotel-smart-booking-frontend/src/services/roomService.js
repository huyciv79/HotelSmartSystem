import axiosInstance from './axiosInstance';

/**
 * Fetch list of room types from backend.
 * GET /api/room-types
 * @returns {Promise<object>} API response containing page list of room types
 */
export const getRoomTypes = async () => {
  const response = await axiosInstance.get('/room-types?size=100');
  return response.data;
};

export const getRoomTypeDetail = async (id) => {
  const response = await axiosInstance.get(`/room-types/${id}`);
  return response.data;
};
