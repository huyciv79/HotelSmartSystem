import axios from 'axios';

const AI_API_BASE_URL = import.meta.env.VITE_AI_API_BASE_URL || 'http://localhost:8000/api/ai';

const aiAxios = axios.create({
  baseURL: AI_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 25000, // LLM requests might take slightly longer
});

/**
 * Creates a default initial booking state template.
 * Automatically loads the active access token if the user is logged in.
 */
export const createInitialBookingState = () => {
  const token = localStorage.getItem('accessToken') || '';
  return {
    step: 'idle',
    hotel: null,
    room_type_id: null,
    room_type_name: '',
    check_in: null,
    check_out: null,
    adults: 1,
    children: 0,
    check_in_method: 'Manual',
    special_requests: '',
    access_token: token,
    booking_result: null,
    error: '',
    chat_history: []
  };
};

/**
 * Ensures the state has the latest authentication token from localStorage.
 */
const syncAccessToken = (state) => {
  const currentState = { ...state };
  currentState.access_token = localStorage.getItem('accessToken') || '';
  return currentState;
};

/**
 * Send a chat message to the AI Assistant.
 * @param {string} message 
 * @param {object} state 
 * @returns {Promise<{response: string, state: object, search_results: object|null}>}
 */
export const chatWithAi = async (message, state) => {
  try {
    const syncedState = syncAccessToken(state);
    const response = await aiAxios.post('/chat', {
      message,
      state: syncedState
    });
    return response.data;
  } catch (error) {
    console.error('Error during AI Chat:', error);
    throw error;
  }
};

/**
 * Directly trigger the booking flow for a selected hotel.
 * @param {object} hotel 
 * @param {object} state 
 * @returns {Promise<{response: string, state: object}>}
 */
export const startBookingWithAi = async (hotel, state) => {
  try {
    const syncedState = syncAccessToken(state);
    const response = await aiAxios.post('/start-booking', {
      hotel,
      state: syncedState
    });
    return response.data;
  } catch (error) {
    console.error('Error starting booking with AI:', error);
    throw error;
  }
};

/**
 * Abort/Cancel the active booking flow.
 * @param {object} state 
 * @returns {Promise<{response: string, state: object}>}
 */
export const cancelBookingWithAi = async (state) => {
  try {
    const syncedState = syncAccessToken(state);
    const response = await aiAxios.post('/cancel-booking', {
      state: syncedState
    });
    return response.data;
  } catch (error) {
    console.error('Error cancelling booking with AI:', error);
    throw error;
  }
};



/**
 * Health check for the AI backend.
 */
export const checkAiHealth = async () => {
  try {
    const response = await aiAxios.get('/health');
    return response.data;
  } catch (error) {
    console.error('AI Backend health check failed:', error);
    return { status: 'down' };
  }
};
