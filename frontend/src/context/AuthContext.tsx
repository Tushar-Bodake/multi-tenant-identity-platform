'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api, AuthResponse, OrganizationDto, UserDto, ProblemDetail } from '@/lib/api';
import { useRouter } from 'next/navigation';

interface AuthContextType {
  user: UserDto | null;
  activeOrganization: OrganizationDto | null;
  organizations: OrganizationDto[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string, tenantId?: string) => Promise<void>;
  logout: () => Promise<void>;
  switchOrganization: (organizationId: string) => Promise<void>;
  setAuth: (auth: AuthResponse) => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [activeOrganization, setActiveOrganization] = useState<OrganizationDto | null>(null);
  const [organizations, setOrganizations] = useState<OrganizationDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  const handleSetAuth = (auth: AuthResponse) => {
    api.setAccessToken(auth.accessToken);
    setUser(auth.user);
    setActiveOrganization(auth.activeOrganization);
    setOrganizations(auth.organizations || []);
  };

  const refreshUser = async () => {
    try {
      const res = await api.fetch<{ user: UserDto; organizations: OrganizationDto[] }>('/auth/me');
      setUser(res.user);
      setOrganizations(res.organizations || []);
      if (res.organizations?.length > 0 && !activeOrganization) {
        setActiveOrganization(res.organizations[0]);
      }
    } catch {
      // Not authenticated or expired
    }
  };

  useEffect(() => {
    // Initial check on mount via refresh endpoint with HttpOnly cookie
    const initAuth = async () => {
      try {
        const res = await fetch('/api/v1/auth/refresh', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
        });
        if (res.ok) {
          const auth = (await res.json()) as AuthResponse;
          handleSetAuth(auth);
        }
      } catch {
        // No valid session
      } finally {
        setIsLoading(false);
      }
    };

    api.setOnTokenRefreshed((token) => {
      if (!token) {
        setUser(null);
        setActiveOrganization(null);
        setOrganizations([]);
      }
    });

    initAuth();
  }, []);

  const login = async (email: string, password: string, tenantId?: string) => {
    const url = tenantId ? `/auth/login?tenantId=${tenantId}` : '/auth/login';
    const auth = await api.fetch<AuthResponse>(url, {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    handleSetAuth(auth);
  };

  const switchOrganization = async (organizationId: string) => {
    setIsLoading(true);
    try {
      // Clear current organization context immediately to prevent data leakage
      setActiveOrganization(null);
      const auth = await api.fetch<AuthResponse>(`/auth/refresh?tenantId=${organizationId}`, {
        method: 'POST',
      });
      handleSetAuth(auth);
      router.push('/dashboard');
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await api.fetch('/auth/logout', { method: 'POST' });
    } catch {
      // Ignore logout network errors
    } finally {
      api.setAccessToken(null);
      setUser(null);
      setActiveOrganization(null);
      setOrganizations([]);
      router.push('/login');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        activeOrganization,
        organizations,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        switchOrganization,
        setAuth: handleSetAuth,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
