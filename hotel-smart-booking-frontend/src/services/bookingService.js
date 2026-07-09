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
  const response = await axiosInstance.post(`/checkin/${bookingId}/manual`);
  return response.data;
};

export const generateQrCheckInToken = async (bookingId) => {
  const response = await axiosInstance.post(`/checkin/${bookingId}/qr-token`);
  return response.data;
};

export const qrCheckInBooking = async (token) => {
  const response = await axiosInstance.post('/checkin/qr', { token });
  return response.data;
};

export const checkOutBooking = async (bookingId) => {
  const response = await axiosInstance.post(`/checkin/${bookingId}/check-out`);
  return response.data;
};

/**
 * FaceID check-in at the hotel lobby.
 * POST /api/bookings/{bookingId}/face-check-in
 */
export const faceCheckInBooking = async (
  bookingId,
  selfieImage,
  challengeImage,
  challengeImage2,
  challengeImage3,
  challengeDirection,
) => {
  const formData = new FormData();
  formData.append('selfieImage', selfieImage);
  formData.append('challengeImage', challengeImage);
  formData.append('challengeImage2', challengeImage2);
  formData.append('challengeImage3', challengeImage3);
  formData.append('challengeDirection', challengeDirection);

  const response = await axiosInstance.post(
    `/checkin/${bookingId}/face`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    },
  );
  return response.data;
};

export const createWalkInBooking = async (walkInData) => {
  const response = await axiosInstance.post('/bookings/walk-in', walkInData);
  return response.data;
};

export const getInvoiceDetails = async (bookingId) => {
  const response = await axiosInstance.get(`/bookings/${bookingId}/invoice`);
  return response.data;
};

export const getStatementPdf = async (bookingId) => {
  const response = await axiosInstance.get(`/bookings/${bookingId}/statement/pdf`, {
    responseType: 'blob'
  });
  return response.data;
};
/**
 * Check whether the camera currently sees exactly one centered, close-enough face.
 * POST /api/bookings/face-readiness
 */
export const checkFaceReadiness = async (selfieImage) => {
  const formData = new FormData();
  formData.append('selfieImage', selfieImage);

  const response = await axiosInstance.post(
    '/checkin/face-readiness',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    },
  );
  return response.data;
};


export const processManualPayment = async (paymentData) => {
  const response = await axiosInstance.post('/payments/manual', paymentData);
  return response.data;
};

export const getAllServices = async () => {
  const response = await axiosInstance.get('/bookings/services/all');
  return response.data;
};

export const addServiceToBooking = async (bookingId, serviceId, quantity, note = '') => {
  const response = await axiosInstance.post(`/bookings/${bookingId}/services`, {
    serviceId,
    quantity,
    note
  });
  return response.data;
};
/**
 * Filter bookings using server-side pagination and criteria.
 * POST /api/bookings/receptionist/filter
 * @param {object} criteria - The filter parameters { status, bookingReference, guestName, guestEmail, page, pageSize, ... }
 * @returns {Promise<object>} API response containing the paginated booking list
 */
export const filterBookings = async (criteria) => {
  const response = await axiosInstance.post('/bookings/receptionist/filter', criteria);
  return response.data;
};

export const cancelBooking = async (bookingId, cancellationReason) => {
  const response = await axiosInstance.delete(`/bookings/receptionist/${bookingId}/cancel`, {
    data: { cancellationReason }
  });
  return response.data;
};

export const updateBooking = async (bookingId, updateData) => {
  const response = await axiosInstance.put(`/bookings/receptionist/${bookingId}`, updateData);
  return response.data;
};

export const customerCancelBooking = async (bookingId, cancellationReason = 'Khách hàng tự hủy trực tuyến') => {
  const response = await axiosInstance.delete(`/bookings/${bookingId}/cancel`, {
    data: { cancellationReason }
  });
  return response.data;
};

