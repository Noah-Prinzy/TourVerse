import { useEffect, useState } from "react";
import { DestinationCard } from "./components/DestinationCard";
import type { Destination } from "./models/Destination";
import { getDestinations } from "./services/api";

function App() {
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getDestinations()
      .then(setDestinations)
      .catch((reason: unknown) => {
        const message =
          reason instanceof Error ? reason.message : "Something went wrong.";
        setError(message);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <header className="site-header">
        <a href="/" className="brand">
          TourVerse
        </a>

        <nav aria-label="Primary navigation">
          <a href="#destinations">Destinations</a>
          <a href="#trips">My trips</a>
          <a href="#about">About</a>
        </nav>
      </header>

      <main>
        <section className="hero">
          <div className="hero-content">
            <p className="eyebrow">Your next journey begins here</p>
            <h1>Discover unforgettable places across Uganda.</h1>
            <p>
              Find natural wonders, cultural experiences and exciting
              adventures for your next trip.
            </p>

            <a className="primary-link" href="#destinations">
              Explore destinations
            </a>
          </div>
        </section>

        <section className="destinations-section" id="destinations">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Featured places</p>
              <h2>Popular destinations</h2>
            </div>

            <input
              type="search"
              aria-label="Search destinations"
              placeholder="Search destinations"
            />
          </div>

          {loading && <p>Loading destinations...</p>}
          {error && <p className="error-message">{error}</p>}

          {!loading && !error && (
            <div className="destination-grid">
              {destinations.map((destination) => (
                <DestinationCard
                  key={destination.id}
                  destination={destination}
                />
              ))}
            </div>
          )}
        </section>
      </main>

      <footer id="about">
        <p>TourVerse · Kotlin, Ktor, React and TypeScript</p>
      </footer>
    </>
  );
}

export default App;
