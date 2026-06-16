import axiosInstance from './axiosInstance';

/**
 * Create a new group room booking.
 * POST /api/bookings/group
 * @param {object} groupBookingData - The group booking data { roomTypeId, checkInDate, checkOutDate, checkInMethod, quantity, numberOfAdults, numberOfChildren, specialRequests }
 * @returns {Promise<object>} API response containing the created group booking details
 */
export const createGroupBooking = async (groupBookingData) => {
  const response = await axiosInstance.post('/bookings/group', groupBookingData);
  return response.data;
};
