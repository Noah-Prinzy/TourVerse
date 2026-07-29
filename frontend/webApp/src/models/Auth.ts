export interface ApiMessage {
  status: string;
  message: string;
}

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  profileImageUrl: string | null;
  bio: string | null;
  role: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface UserProfile extends User {
  nationality: string | null;
  travelInterests: string[];
  profilePublic: boolean;
  updatedAt: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  bio?: string;
  nationality?: string;
  travelInterests?: string[];
  profilePublic?: boolean;
}
