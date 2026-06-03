export async function getProfile() {
  return {
    id: '1',
    name: 'Guest User',
    stylePreferences: ['Minimalist', 'Classic'],
    bodyType: null,
    fitPreference: null,
    favoriteColors: [],
    sizeProfile: {},
  }
}

export async function updateProfile(data) {
  return { ...data, id: '1' }
}
