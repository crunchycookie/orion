import { render, screen } from '@testing-library/react';
import MasterCanvas from './MasterCanvas';

test('renders learn react link', () => {
  render(<MasterCanvas />);
  const linkElement = screen.getByText(/learn react/i);
  expect(linkElement).toBeInTheDocument();
});
