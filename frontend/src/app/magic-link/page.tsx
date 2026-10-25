'use client';

import React, { useState, useEffect, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { Shield, Mail, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { api, AuthResponse } from '@/lib/api';

function MagicLinkContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const tokenParam = searchParams.get('token');
  const { setAuth } = useAuth();

  const [email, setEmail] = useState('');
  const [token, setToken] = useState(tokenParam || '');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleVerify = async (tokenToVerify: string) => {
    if (!tokenToVerify.trim()) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      const auth = await api.fetch<AuthResponse>('/auth/magic-link/verify', {
        method: 'POST',
        body: JSON.stringify({ token: tokenToVerify.trim() }),
      });
      setAuth(auth);
      router.push('/dashboard');
    } catch (err: any) {
      setErrorMessage(err.detail || 'Magic link verification failed. The link may have expired or was already used.');
    } finally {
      setLoading(false);
    }
  };

  const handleRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMessage(null);

    try {
      await api.fetch('/auth/magic-link/request', {
        method: 'POST',
        body: JSON.stringify({ email }),
      });
      setSent(true);
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to request magic link.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tokenParam) {
      handleVerify(tokenParam);
    }
  }, [tokenParam]);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <Link href="/" className="flex items-center justify-center gap-2 mb-2">
          <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
            <Shield className="w-6 h-6" />
          </div>
          <span className="text-2xl font-bold tracking-tight text-slate-900">SecureGate</span>
        </Link>
        <h2 className="text-center text-xl font-bold tracking-tight text-slate-800">
          Passwordless Magic Link
        </h2>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow-sm border border-slate-200 rounded-2xl sm:px-10 text-center">
          {loading ? (
            <div className="py-8 space-y-3">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-sky-600" />
              <p className="text-sm text-slate-600 font-medium">Authenticating via Magic Link...</p>
            </div>
          ) : sent ? (
            <div className="py-4 space-y-3">
              <div className="mx-auto w-10 h-10 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <h3 className="text-sm font-semibold text-slate-900">Sign-in Link Dispatched</h3>
              <p className="text-xs text-slate-500 leading-relaxed">
                If an account exists for <strong className="text-slate-700">{email}</strong>, a secure sign-in link has been sent.
              </p>
              <div className="pt-3">
                <Link href="/login" className="btn-secondary w-full text-xs">
                  Return to Sign In
                </Link>
              </div>
            </div>
          ) : (
            <div className="space-y-4 text-left">
              {errorMessage && (
                <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{errorMessage}</span>
                </div>
              )}

              {token ? (
                <div className="space-y-3">
                  <p className="text-xs text-slate-500">
                    A token was detected. Click below to verify and complete sign in.
                  </p>
                  <button
                    type="button"
                    onClick={() => handleVerify(token)}
                    className="btn-primary w-full"
                  >
                    Authenticate with Token
                  </button>
                </div>
              ) : (
                <form onSubmit={handleRequest} className="space-y-4">
                  <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">
                      Email Address
                    </label>
                    <div className="relative">
                      <Mail className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                      <input
                        type="email"
                        required
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="alice@company.com"
                        className="input-field pl-9"
                      />
                    </div>
                  </div>
                  <button
                    type="submit"
                    className="btn-primary w-full"
                  >
                    Send Magic Link
                  </button>
                </form>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function MagicLinkPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-sky-600" />
      </div>
    }>
      <MagicLinkContent />
    </Suspense>
  );
}
