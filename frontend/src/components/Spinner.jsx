/**
 * Inline loading spinner.
 * @param {'light'|'dark'} variant - use "dark" on light backgrounds.
 */
export default function Spinner({ variant = 'light', size }) {
  const classes = ['spinner'];
  if (variant === 'dark') classes.push('dark');

  const style = size ? { width: size, height: size } : undefined;

  return <span className={classes.join(' ')} style={style} role="status" aria-label="Loading" />;
}
