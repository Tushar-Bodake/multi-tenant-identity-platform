'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Shield, CheckCircle2, AlertCircle, ArrowRight, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';

export default function VerifyEmailPage() {
  const searchParams = useSearchParams();
  const tokenParam = searchParams.get('token');

  const [token, setToken] = useState(tokenParam || '');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleVerify = async (tokenToVerify: string) => {
    if (!tokenToVerify.trim()) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      await api.fetch('/auth/verify-email', {
        method: 'POST',
        body: JSON.stringify({ token: tokenToVerify.trim() }),
      });
      setSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.detail || 'Failed to verify email. The token may be expired or already used.');
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
          Email Ownership Verification
        </h2>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow-sm border border-slate-200 rounded-2xl sm:px-10 text-center">
          {loading ? (
            <div className="py-8 space-y-3">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-sky-600" />
              <p className="text-sm text-slate-600 font-medium">Verifying your token...</p>
            </div>
          ) : success ? (
            <div className="py-6 space-y-4">
              <div className="mx-auto w-12 h-12 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <h3 className="text-base font-bold text-slate-900">Email Verified Successfully!</h3>
              <p className="text-xs text-slate-500">
                Your account is now activated. You have full access to organization management.
              </p>
              <Link href="/login" className="btn-primary w-full inline-flex items-center justify-center gap-2 mt-4">
                Sign in to Dashboard <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          ) : (
            <div className="space-y-4 text-left">
              {errorMessage && (
                <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{errorMessage}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Verification Token
                </label>
                <input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  placeholder="Paste verification token here"
                  className="input-field"
                />
              </div>

              <button
                type="button"
                onClick={() => handleVerify(token)}
                disabled={!token.trim()}
                className="btn-primary w-full"
              >
                Confirm Verification
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
