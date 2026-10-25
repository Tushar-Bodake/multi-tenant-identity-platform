'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { api, ProblemDetail } from '@/lib/api';
import { Shield, Mail, CheckCircle2, AlertCircle } from 'lucide-react';

export default function MagicLinkRequestPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await api.fetch('/auth/magic-link/request', {
        method: 'POST',
        body: JSON.stringify({ email }),
      });
      setSubmitted(true);
    } catch (err: any) {
      const prob = err as ProblemDetail;
      setError(prob.detail || 'Failed to request magic link');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link href="/" className="inline-flex items-center gap-2 mb-4">
          <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
            <Shield className="w-6 h-6" />
          </div>
          <span className="text-2xl font-bold tracking-tight text-slate-900">SecureGate</span>
        </Link>
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">Sign in with Magic Link</h2>
        <p className="mt-2 text-sm text-slate-600">Passwordless sign-in delivered directly to your inbox</p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm border border-slate-200 sm:rounded-xl sm:px-10">
          {submitted ? (
            <div className="text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 mb-4">
                <CheckCircle2 className="h-6 w-6 text-emerald-600" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">Check your email</h3>
              <p className="text-sm text-slate-600 mb-6">
                A single-use authentication link was sent to <strong>{email}</strong>. It will expire in 15 minutes.
              </p>
              <Link href="/login" className="btn-secondary w-full">
                Back to Sign In
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div className="p-4 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
                  <div>{error}</div>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Email address</label>
                <div className="relative">
                  <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                    <Mail className="h-4 w-4" />
                  </div>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="input-field pl-9"
                    placeholder="you@company.com"
                  />
                </div>
              </div>

              <button type="submit" disabled={loading} className="btn-primary w-full">
                {loading ? 'Sending link...' : 'Send Magic Link'}
              </button>

              <div className="text-center mt-4">
                <Link href="/login" className="text-sm font-medium text-slate-600 hover:text-sky-600">
                  Or sign in with password
                </Link>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
