export function portalPathFor(user) {
  return user.role === 'ADMIN' ? '/admin' : '/dashboard';
}
