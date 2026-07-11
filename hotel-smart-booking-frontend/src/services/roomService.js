import axiosInstance from './axiosInstance';

/**
 * Fetch list of room types from backend.
 * GET /api/room-types
 * @returns {Promise<object>} API response containing page list of room types
 */
export const getRoomTypes = async (status = '', keyword = '') => {
  let url = status ? `/room-types?size=100&status=${status}` : '/room-types?size=100';
  if (keyword) {
    url += `&keyword=${encodeURIComponent(keyword)}`;
  }
  const response = await axiosInstance.get(url);
  return response.data;
};

export const getRoomTypeDetail = async (id) => {
  const response = await axiosInstance.get(`/room-types/${id}`);
  return response.data;
};
