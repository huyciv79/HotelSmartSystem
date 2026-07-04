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
