import axiosInstance from './axiosInstance';
import { clearRoomTypesCache } from './roomService';

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
  clearRoomTypesCache();
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
  clearRoomTypesCache();
  return response.data;
};

/**
 * Delete a room type (soft delete).
 * DELETE /api/room-types/{id}
 */
export const deleteRoomType = async (id) => {
  const response = await axiosInstance.delete(`/room-types/${id}`);
  clearRoomTypesCache();
  return response.data;
};

/**
 * Fetch list of individual rooms from backend.
 * GET /api/rooms
 */
export const getRooms = async (params = {}) => {
  const query = new URLSearchParams();
  Object.keys(params).forEach(key => {
    if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
      query.append(key, params[key]);
    }
  });
  const response = await axiosInstance.get(`/rooms?${query.toString()}`);
  return response.data;
};

/**
 * Update an existing individual room's details.
 * PUT /api/rooms/{id}
 */
export const updateRoom = async (id, data) => {
  const response = await axiosInstance.put(`/rooms/${id}`, data);
  return response.data;
};

/**
 * Create a new individual room.
 * POST /api/rooms
 */
export const createRoom = async (data) => {
  const response = await axiosInstance.post('/rooms', data);
  return response.data;
};
