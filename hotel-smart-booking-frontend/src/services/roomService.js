import axiosInstance from './axiosInstance';

const ROOM_TYPES_CACHE_TTL_MS = 60_000;
const roomTypesCache = new Map();

const createRoomTypesUrl = (status, keyword) => {
  let url = status ? `/room-types?size=100&status=${status}` : '/room-types?size=100';
  if (keyword) {
    url += `&keyword=${encodeURIComponent(keyword)}`;
  }
  return url;
};

/**
 * Fetch list of room types from backend.
 * GET /api/room-types
 * @returns {Promise<object>} API response containing page list of room types
 */
export const getRoomTypes = (status = '', keyword = '') => {
  const cacheKey = `${status}|${keyword}`;
  const cached = roomTypesCache.get(cacheKey);
  const now = Date.now();

  if (cached?.data && now - cached.updatedAt < ROOM_TYPES_CACHE_TTL_MS) {
    return Promise.resolve(cached.data);
  }
  if (cached?.promise) {
    return cached.promise;
  }

  const request = axiosInstance.get(createRoomTypesUrl(status, keyword))
    .then((response) => {
      roomTypesCache.set(cacheKey, {
        data: response.data,
        updatedAt: Date.now(),
      });
      return response.data;
    })
    .catch((error) => {
      roomTypesCache.delete(cacheKey);
      throw error;
    });

  roomTypesCache.set(cacheKey, { promise: request });
  return request;
};

export const getRoomTypeDetail = async (id) => {
  const response = await axiosInstance.get(`/room-types/${id}`);
  return response.data;
};

/**
 * Get the lowest remaining room inventory for a room type across a stay.
 * GET /api/bookings/availability
 */
export const getRoomAvailability = async (roomTypeId, checkInDate, checkOutDate) => {
  const response = await axiosInstance.get('/bookings/availability', {
    params: { roomTypeId, checkInDate, checkOutDate },
  });
  return response.data;
};
