import axiosInstance from './axiosInstance';

/**
 * Create a new room booking.
 * POST /api/bookings
 * @param {object} bookingData - The booking data { roomTypeId, checkInDate, checkOutDate, checkInMethod, specialRequests }
 * @returns {Promise<object>} API response containing the created booking details
 */
export const createBooking = async (bookingData) => {
  const response = await axiosInstance.post('/bookings', bookingData);
  return response.data;
};

/**
 * Fetch booking details by ID.
 * GET /api/bookings/{id}
 * @param {number|string} id - The booking ID
 * @returns {Promise<object>} API response containing the booking details
 */
export const getBookingDetail = async (id) => {
  const response = await axiosInstance.get(`/bookings/${id}`);
  return response.data;
};

/**
 * Fetch booking history for the current authenticated user.
 * GET /api/bookings/history
 * @returns {Promise<object>} API response containing the booking history list
 */
export const getBookingHistory = async () => {
  const response = await axiosInstance.get('/bookings/history');
  return response.data;
};

export const getAllBookings = async () => {
  const response = await axiosInstance.get('/bookings/all');
  return response.data;
};

export const checkInBooking = async (bookingId) => {
  const response = await axiosInstance.post(`/bookings/${bookingId}/check-in`);
  return response.data;
};

export const checkOutBooking = async (bookingId) => {
  const response = await axiosInstance.post(`/bookings/${bookingId}/check-out`);
  return response.data;
};

/**
 * FaceID check-in at the hotel lobby.
 * POST /api/bookings/{bookingId}/face-check-in
 */
export const faceCheckInBooking = async (
  bookingId,
  selfieImage,
  leftImage,
  rightImage,
  upImage,
  downImage,
) => {
  const formData = new FormData();
  formData.append('selfieImage', selfieImage);
  formData.append('leftImage', leftImage);
  formData.append('rightImage', rightImage);
  formData.append('upImage', upImage);
  formData.append('downImage', downImage);

  const response = await axiosInstance.post(
    `/bookings/${bookingId}/face-check-in`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    },
  );
  return response.data;
};

export const getStatementPdf = async (bookingId) => {
  const response = await axiosInstance.get(`/bookings/${bookingId}/statement/pdf`, {
    responseType: 'blob'
  });
  return response.data;
};
