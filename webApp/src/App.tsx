import { useEffect, useState } from "react";
import { DestinationCard } from "./components/DestinationCard";
import type {
  Destination,
  DestinationSortField,
  SortDirection,
} from "./models/Destination";
import { getDestinations } from "./services/api";

const PAGE_SIZES = [10, 20, 50];

function App() {
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [country, setCountry] = useState("");
  const [city, setCity] = useState("");
  const [category, setCategory] = useState("");
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [sortBy, setSortBy] =
    useState<DestinationSortField>("createdAt");
  const [sortDirection, setSortDirection] =
    useState<SortDirection>("desc");
  const [retryVersion, setRetryVersion] = useState(0);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setPage(1);
      setSearch(searchInput);
    }, 350);
    return () => window.clearTimeout(timeout);
  }, [searchInput]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");

    getDestinations(
      {
        search,
        country,
        city,
        category,
        page,
        size,
        sortBy,
        sortDirection,
      },
      controller.signal,
    )
      .then((response) => {
        setDestinations(response.items);
        setPage(response.page);
        setSize(response.size);
        setTotalItems(response.totalItems);
        setTotalPages(response.totalPages);
      })
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") {
          return;
        }
        const message =
          reason instanceof Error
            ? reason.message
            : "Unable to load destinations. Please try again.";
        setDestinations([]);
        setTotalItems(0);
        setTotalPages(0);
        setError(message);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [
    search,
    country,
    city,
    category,
    page,
    size,
    sortBy,
    sortDirection,
    retryVersion,
  ]);

  const updateFilter = (
    setter: React.Dispatch<React.SetStateAction<string>>,
    value: string,
  ) => {
    setPage(1);
    setter(value);
  };

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
            <p className="result-count" aria-live="polite">
              {totalItems} {totalItems === 1 ? "destination" : "destinations"}
            </p>
          </div>

          <div className="destination-controls">
            <label>
              <span>Search</span>
              <input
                type="search"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Search destinations"
              />
            </label>
            <label>
              <span>Country</span>
              <input
                value={country}
                onChange={(event) =>
                  updateFilter(setCountry, event.target.value)
                }
                placeholder="Any country"
              />
            </label>
            <label>
              <span>City</span>
              <input
                value={city}
                onChange={(event) => updateFilter(setCity, event.target.value)}
                placeholder="Any city"
              />
            </label>
            <label>
              <span>Category</span>
              <input
                value={category}
                onChange={(event) =>
                  updateFilter(setCategory, event.target.value)
                }
                placeholder="Any category"
              />
            </label>
            <label>
              <span>Sort by</span>
              <select
                value={sortBy}
                onChange={(event) => {
                  setPage(1);
                  setSortBy(event.target.value as DestinationSortField);
                }}
              >
                <option value="createdAt">Newest</option>
                <option value="updatedAt">Recently updated</option>
                <option value="name">Name</option>
                <option value="country">Country</option>
                <option value="city">City</option>
                <option value="category">Category</option>
              </select>
            </label>
            <label>
              <span>Direction</span>
              <select
                value={sortDirection}
                onChange={(event) => {
                  setPage(1);
                  setSortDirection(event.target.value as SortDirection);
                }}
              >
                <option value="desc">Descending</option>
                <option value="asc">Ascending</option>
              </select>
            </label>
            <label>
              <span>Page size</span>
              <select
                value={size}
                onChange={(event) => {
                  setPage(1);
                  setSize(Number(event.target.value));
                }}
              >
                {PAGE_SIZES.map((pageSize) => (
                  <option key={pageSize} value={pageSize}>
                    {pageSize}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {loading && (
            <p className="status-message" role="status">
              Loading destinations...
            </p>
          )}
          {!loading && error && (
            <div className="error-message" role="alert">
              <p>{error}</p>
              <button
                type="button"
                onClick={() => setRetryVersion((value) => value + 1)}
              >
                Try again
              </button>
            </div>
          )}
          {!loading && !error && destinations.length === 0 && (
            <p className="empty-message" role="status">
              No destinations match the current search and filters.
            </p>
          )}
          {!loading && !error && destinations.length > 0 && (
            <div className="destination-grid">
              {destinations.map((destination) => (
                <DestinationCard
                  key={destination.id}
                  destination={destination}
                />
              ))}
            </div>
          )}

          {!error && (
            <nav className="pagination" aria-label="Destination pages">
              <button
                type="button"
                disabled={loading || page <= 1}
                onClick={() => setPage((value) => Math.max(1, value - 1))}
              >
                Previous
              </button>
              <span>
                Page {totalPages === 0 ? 1 : page} of{" "}
                {Math.max(1, totalPages)}
              </span>
              <button
                type="button"
                disabled={loading || totalPages === 0 || page >= totalPages}
                onClick={() =>
                  setPage((value) => Math.min(totalPages, value + 1))
                }
              >
                Next
              </button>
            </nav>
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
