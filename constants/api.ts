// services/api.ts
import { Platform } from 'react-native';

const LOCAL_IP = '192.168.1.60';
const PORT = '8080';

const BASE_URL = Platform.select({
  android: `http://${LOCAL_IP}:${PORT}/api`,
  ios: `http://${LOCAL_IP}:${PORT}/api`,
  default: `http://localhost:${PORT}/api`,
});

export const API_URLS = {
  ROUTES: `${BASE_URL}/route`,
  ROUTE_BY_ID: (id: number) => `${BASE_URL}/route/${id}`,
  
};

export async function fetchRoutes() {
  try {
    const response = await fetch(API_URLS.ROUTES!);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('API Error (fetchRoutes):', error);
    throw error;
  }
}

export async function fetchRouteDetails(id: number) {
  try {
    const url = API_URLS.ROUTE_BY_ID(id);
    const response = await fetch(url);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('API Error (fetchRouteDetails):', error);
    throw error;
  }
}