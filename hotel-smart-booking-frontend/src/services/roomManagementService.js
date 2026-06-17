import axiosInstance from './axiosInstance';

/**
 * Create a new room type.
 * POST /api/room-types (Multipart Form Data)
 */
export const createRoomType = async (formData) => {
  const response = await axiosInstance.post('/room-types', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

/**
 * Update an existing room type.
 * PUT /api/room-types/{id} (Multipart Form Data)
 */
export const updateRoomType = async (id, formData) => {
  const response = await axiosInstance.put(`/room-types/${id}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

/**
 * Delete a room type (soft delete).
 * DELETE /api/room-types/{id}
 */
export const deleteRoomType = async (id) => {
  const response = await axiosInstance.delete(`/room-types/${id}`);
  return response.data;
};
