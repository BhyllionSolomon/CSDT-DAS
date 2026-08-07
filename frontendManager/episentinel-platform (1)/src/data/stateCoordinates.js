// Approximate centroid coordinates for Nigerian states, used only to plot
// markers on NigeriaMap. The backend's state-distribution response only
// needs to identify a state by name/code — it does not need to send lat/lng.
// Extend this list as new states appear in the live API response.

export const STATE_COORDINATES = {
  Lagos: { lat: 6.5244, lng: 3.3792 },
  Kano: { lat: 12.0022, lng: 8.5920 },
  Oyo: { lat: 8.1574, lng: 3.6147 },
  Rivers: { lat: 4.8156, lng: 7.0498 },
  Borno: { lat: 11.8333, lng: 13.1500 },
  Kaduna: { lat: 10.5105, lng: 7.4165 },
  Enugu: { lat: 6.5244, lng: 7.5086 },
  Sokoto: { lat: 13.0059, lng: 5.2476 },
  Anambra: { lat: 6.2209, lng: 7.0716 },
  Plateau: { lat: 9.2182, lng: 9.5179 },
  'Cross River': { lat: 5.9631, lng: 8.3320 },
  Benue: { lat: 7.3369, lng: 8.7404 },
}

export function getStateCoordinates(stateName) {
  return STATE_COORDINATES[stateName] || null
}
