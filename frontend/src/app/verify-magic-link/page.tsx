'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { api, AuthResponse, ProblemDetail } from '@/lib/api';
import { Shield, Loader2, AlertCircle } from 'lucide-react';

export default function VerifyMagicLinkPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const { setAuth } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setError('Missing magic link authentication token.');
      setLoading(false);
      return;
    }

    const verify = async () => {
      try {
        const auth = await api.fetch<AuthResponse>('/auth/magic-link/verify', {
          method: 'POST',
          body: JSON.stringify({ token }),
        });
        setAuth(auth);
        router.push('/dashboard');
      } catch (err: any) {
        const prob = err as ProblemDetail;
        setError(prob.detail || 'Magic link is invalid or has expired.');
        setLoading(false);
      }
    };

    verify();
  }, [token, router, setAuth]);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link href="/" className="inline-flex items-center gap-2 mb-4">
          <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
            <Shield className="w-6 h-6" />
          </div>
          <span className="text-2xl font-bold tracking-tight text-slate-900">SecureGate</span>
        </Link>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm border border-slate-200 sm:rounded-xl sm:px-10 text-center">
          {loading && (
            <div className="py-8">
              <Loader2 className="w-8 h-8 text-sky-600 animate-spin mx-auto mb-4" />
              <p className="text-slate-600 text-sm">Authenticating your magic link session...</p>
            </div>
          )}

          {!loading && error && (
            <div>
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rose-100 mb-4">
                <AlertCircle className="h-6 w-6 text-rose-600" />
              </div>
              <h2 className="text-xl font-bold text-slate-900 mb-2">Sign-In Failed</h2>
              <p className="text-sm text-rose-600 mb-6">{error}</p>
              <Link href="/magic-link" className="btn-secondary w-full">
                Request a new Magic Link
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
