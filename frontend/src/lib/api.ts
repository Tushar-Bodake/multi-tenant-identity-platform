export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail: string;
  instance?: string;
  errors?: Record<string, string>;
  timestamp?: string;
}

export interface UserDto {
  id: string;
  fullName: string;
  email: string;
  emailVerified: boolean;
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED';
}

export interface OrganizationDto {
  id: string;
  name: string;
  slug: string;
  status: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
  activeOrganization: OrganizationDto | null;
  organizations: OrganizationDto[];
}

export interface MemberDto {
  userId: string;
  fullName: string;
  email: string;
  role: string;
  isCurrentUser: boolean;
}

export interface ApplicationDto {
  id: string;
  organizationId: string;
  name: string;
  clientId: string;
  clientSecretHash: string;
  redirectUris?: string;
  createdAt: string;
}

export interface ApplicationCreatedDto {
  id: string;
  name: string;
  clientId: string;
  clientSecret: string;
  redirectUris?: string;
}

export interface AuditEventDto {
  id: string;
  timestamp: string;
  actorId?: string;
  tenantId?: string;
  action: string;
  targetType: string;
  targetId?: string;
  outcome: string;
  correlationId: string;
  metadataJson?: string;
  prevHash: string;
  currentHash: string;
}

export interface AuditVerificationResult {
  valid: boolean;
  eventsVerified: number;
  brokenEventId?: string;
  brokenIndex?: number;
  errorMessage?: string;
}

export interface PasskeyDto {
  id: string;
  credentialId: string;
  nickname: string;
  signCount: number;
  createdAt: string;
  lastUsedAt?: string;
}

class ApiClient {
  private inMemoryAccessToken: string | null = null;
  private onTokenRefreshedCallback: ((token: string | null) => void) | null = null;

  public setAccessToken(token: string | null) {
    this.inMemoryAccessToken = token;
  }

  public getAccessToken(): string | null {
    return this.inMemoryAccessToken;
  }

  public setOnTokenRefreshed(callback: (token: string | null) => void) {
    this.onTokenRefreshedCallback = callback;
  }

  public async fetch<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});
    if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    if (this.inMemoryAccessToken) {
      headers.set('Authorization', `Bearer ${this.inMemoryAccessToken}`);
    }

    const config: RequestInit = {
      ...options,
      headers,
      credentials: 'include', // Include HttpOnly cookies
    };

    let response = await fetch(`/api/v1${path}`, config);

    // If 401 Unauthorized, attempt refresh once
    if (response.status === 401 && !path.includes('/auth/login') && !path.includes('/auth/refresh')) {
      try {
        const refreshRes = await fetch('/api/v1/auth/refresh', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
        });

        if (refreshRes.ok) {
          const authData = (await refreshRes.json()) as AuthResponse;
          this.setAccessToken(authData.accessToken);
          if (this.onTokenRefreshedCallback) {
            this.onTokenRefreshedCallback(authData.accessToken);
          }
          headers.set('Authorization', `Bearer ${authData.accessToken}`);
          response = await fetch(`/api/v1${path}`, { ...config, headers });
        } else {
          this.setAccessToken(null);
          if (this.onTokenRefreshedCallback) {
            this.onTokenRefreshedCallback(null);
          }
        }
      } catch {
        this.setAccessToken(null);
      }
    }

    if (!response.ok) {
      let problem: ProblemDetail;
      try {
        problem = await response.json();
      } catch {
        problem = {
          status: response.status,
          detail: response.statusText || 'An unexpected error occurred',
        };
      }
      throw problem;
    }

    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  }
}

export const api = new ApiClient();
