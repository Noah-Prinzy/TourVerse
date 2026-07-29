import { Link } from "react-router-dom";

export const NotFoundPage = () => <section className="simple-page"><h1>Page not found</h1><p>The page you requested does not exist.</p><Link to="/destinations">Return to destinations</Link></section>;
export const DestinationDetailsPlaceholder = () => <section className="simple-page"><h1>Destination details</h1><p>This route is ready for the next catalogue phase.</p></section>;
export const TripsPlaceholder = () => <section className="simple-page"><h1>My trips</h1><p>Trip management is coming in the next phase.</p></section>;
