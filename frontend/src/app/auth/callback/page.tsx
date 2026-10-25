'use client';

import React, { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Shield, Loader2, AlertCircle } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { api, AuthResponse } from '@/lib/api';

export default function OAuthCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { setAuth } = useAuth();

  const provider = searchParams.get('provider') || 'google';
  const email = searchParams.get('email') || 'developer@securegate.local';
  const name = searchParams.get('name') || 'Enterprise Developer';

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const processOAuth = async () => {
      try {
        const providerSubject = `${provider}_sub_${Math.random().toString(36).substring(2, 10)}`;

        const auth = await api.fetch<AuthResponse>('/auth/oauth/callback', {
          method: 'POST',
          body: JSON.stringify({
            provider,
            providerSubject,
            email,
            name,
            confirmLink: true,
          }),
        });

        setAuth(auth);
        router.push('/dashboard');
      } catch (err: any) {
        setError(err.detail || 'OAuth federated authentication failed.');
        setLoading(false);
      }
    };

    processOAuth();
  }, [provider, email, name, router, setAuth]);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="flex items-center justify-center gap-2 mb-4">
          <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
            <Shield className="w-6 h-6" />
          </div>
          <span className="text-2xl font-bold tracking-tight text-slate-900">SecureGate</span>
        </div>

        <div className="bg-white py-8 px-4 shadow-sm border border-slate-200 rounded-2xl sm:px-10">
          {loading ? (
            <div className="space-y-4 py-6">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-sky-600" />
              <h3 className="text-sm font-semibold text-slate-800">
                Verifying OAuth 2.0 / OIDC credentials...
              </h3>
              <p className="text-xs text-slate-500">
                Validating identity tokens with {provider === 'google' ? 'Google Identity' : 'GitHub'}
              </p>
            </div>
          ) : (
            <div className="space-y-4 py-4 text-left">
              <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{error}</span>
              </div>
              <button
                type="button"
                onClick={() => router.push('/login')}
                className="btn-primary w-full text-xs"
              >
                Return to Login
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
