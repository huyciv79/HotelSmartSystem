import axiosInstance from './axiosInstance';

/**
 * Fetch all services (not deleted).
 * GET /api/services
 */
export const getServices = async () => {
  const response = await axiosInstance.get('/services');
  return response.data;
};

/**
 * Fetch details of a single service by ID.
 * GET /api/services/{id}
 */
export const getServiceById = async (id) => {
  const response = await axiosInstance.get(`/services/${id}`);
  return response.data;
};

/**
 * Create a new service.
 * POST /api/services
 */
export const createService = async (serviceData) => {
  const response = await axiosInstance.post('/services', serviceData);
  return response.data;
};

/**
 * Update an existing service.
 * PUT /api/services/{id}
 */
export const updateService = async (id, serviceData) => {
  const response = await axiosInstance.put(`/services/${id}`, serviceData);
  return response.data;
};

/**
 * Delete a service (soft delete).
 * DELETE /api/services/{id}
 */
export const deleteService = async (id) => {
  const response = await axiosInstance.delete(`/services/${id}`);
  return response.data;
};

/**
 * Submit an in-stay service request.
 * POST /api/stay-adjustments/service-request
 */
export const submitCustomerServiceRequest = async (payload) => {
  const response = await axiosInstance.post('/stay-adjustments/service-request', payload);
  return response.data;
};

/**
 * Get pending service requests (Staff).
 * GET /api/stay-adjustments/service-request/pending
 */
export const getPendingServiceRequests = async () => {
  const response = await axiosInstance.get('/stay-adjustments/service-request/pending');
  return response.data;
};

/**
 * Get service requests by booking ID (Guest & Staff).
 * GET /api/stay-adjustments/service-request/booking/{bookingId}
 */
export const getServiceRequestsByBooking = async (bookingId) => {
  const response = await axiosInstance.get(`/stay-adjustments/service-request/booking/${bookingId}`);
  return response.data;
};

/**
 * Approve a service request (Staff).
 * POST /api/stay-adjustments/service-request/approve/{requestId}
 */
export const approveCustomerServiceRequest = async (requestId) => {
  const response = await axiosInstance.post(`/stay-adjustments/service-request/approve/${requestId}`);
  return response.data;
};

/**
 * Reject a service request (Staff).
 * POST /api/stay-adjustments/service-request/reject/{requestId}
 */
export const rejectCustomerServiceRequest = async (requestId, rejectionReason) => {
  const response = await axiosInstance.post(
    `/stay-adjustments/service-request/reject/${requestId}`,
    null,
    { params: { rejectionReason } }
  );
  return response.data;
};
