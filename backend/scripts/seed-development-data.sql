-- Explicit development-only seed data for TourVerse.
-- Run manually against a disposable/local development database.
-- Never run this script automatically or against production.

BEGIN;

INSERT INTO categories (id, name, slug, description)
VALUES
    (gen_random_uuid(), 'Hiking', 'hiking', 'Walking, trekking, and mountain destinations'),
    (gen_random_uuid(), 'Water', 'water', 'Lakes, rivers, waterfalls, and islands'),
    (gen_random_uuid(), 'Urban', 'urban', 'Cities and town-based experiences'),
    (gen_random_uuid(), 'Religious', 'religious', 'Places associated with faith and religious heritage')
ON CONFLICT (slug) DO NOTHING;

WITH seed(name, country, city, description, category) AS (
    VALUES
        ('Bwindi Impenetrable National Park', 'Uganda', 'Kanungu', 'A forest destination used for developing wildlife and nature discovery experiences.', 'Wildlife'),
        ('Murchison Falls National Park', 'Uganda', 'Masindi', 'A national park destination used for developing river, landscape, and wildlife discovery experiences.', 'Wildlife'),
        ('Queen Elizabeth National Park', 'Uganda', 'Kasese', 'A national park destination used for developing wildlife and landscape discovery experiences.', 'Wildlife'),
        ('Kidepo Valley National Park', 'Uganda', 'Kaabong', 'A national park destination used for developing remote landscape and wildlife experiences.', 'Wildlife'),
        ('Mgahinga Gorilla National Park', 'Uganda', 'Kisoro', 'A highland park destination used for developing nature, hiking, and wildlife experiences.', 'Wildlife'),
        ('Mount Elgon National Park', 'Uganda', 'Mbale', 'A mountain destination used for developing hiking and outdoor travel experiences.', 'Hiking'),
        ('Rwenzori Mountains National Park', 'Uganda', 'Kasese', 'A mountain destination used for developing trekking and nature travel experiences.', 'Hiking'),
        ('Lake Mburo National Park', 'Uganda', 'Mbarara', 'A national park destination used for developing wildlife and lakeside experiences.', 'Wildlife'),
        ('Kibale National Park', 'Uganda', 'Fort Portal', 'A forest destination used for developing nature and wildlife experiences.', 'Nature'),
        ('Semuliki National Park', 'Uganda', 'Bundibugyo', 'A national park destination used for developing forest and nature experiences.', 'Nature'),
        ('Jinja', 'Uganda', 'Jinja', 'An urban destination used for developing river and adventure travel experiences.', 'Urban'),
        ('Sipi Falls', 'Uganda', 'Kapchorwa', 'A waterfall destination used for developing hiking and nature experiences.', 'Water'),
        ('Lake Bunyonyi', 'Uganda', 'Kabale', 'A lake destination used for developing relaxing water and landscape experiences.', 'Water'),
        ('Ssese Islands', 'Uganda', 'Kalangala', 'An island destination used for developing lakeside and nature experiences.', 'Water'),
        ('Kasubi Tombs', 'Uganda', 'Kampala', 'A heritage destination used for developing cultural and historical experiences.', 'Historical'),
        ('Uganda Museum', 'Uganda', 'Kampala', 'A museum destination used for developing cultural and historical discovery.', 'Culture'),
        ('Nyero Rock Paintings', 'Uganda', 'Kumi', 'A heritage destination used for developing historical and cultural experiences.', 'Historical'),
        ('Source of the Nile', 'Uganda', 'Jinja', 'A river destination used for developing water and sightseeing experiences.', 'Water'),
        ('Ziwa Rhino Sanctuary', 'Uganda', 'Nakasongola', 'A sanctuary destination used for developing wildlife discovery experiences.', 'Wildlife'),
        ('Entebbe Botanical Gardens', 'Uganda', 'Entebbe', 'A garden destination used for developing accessible nature experiences.', 'Nature'),
        ('Kampala', 'Uganda', 'Kampala', 'A capital-city destination used for developing urban and cultural experiences.', 'Urban'),
        ('Fort Portal', 'Uganda', 'Fort Portal', 'A city destination used for developing urban access to nearby nature experiences.', 'Urban'),
        ('Kabale', 'Uganda', 'Kabale', 'A highland town destination used for developing urban and landscape experiences.', 'Urban'),
        ('Gaddafi National Mosque', 'Uganda', 'Kampala', 'A landmark destination used for developing religious and urban heritage experiences.', 'Religious'),
        ('Namugongo Martyrs Shrine', 'Uganda', 'Wakiso', 'A faith destination used for developing religious heritage experiences.', 'Religious'),
        ('Baha''i Temple', 'Uganda', 'Kampala', 'A faith and garden destination used for developing religious heritage experiences.', 'Religious'),
        ('Itanda Falls', 'Uganda', 'Jinja', 'A river destination used for developing water and adventure experiences.', 'Adventure'),
        ('Pian Upe Wildlife Reserve', 'Uganda', 'Nakapiripirit', 'A reserve destination used for developing wildlife and landscape experiences.', 'Wildlife'),
        ('Mabamba Bay', 'Uganda', 'Wakiso', 'A wetland destination used for developing water and nature experiences.', 'Nature'),
        ('Amabere Caves', 'Uganda', 'Fort Portal', 'A natural destination used for developing geology, culture, and walking experiences.', 'Nature'),
        ('Tororo Rock', 'Uganda', 'Tororo', 'A rock landmark used for developing hiking and landscape experiences.', 'Hiking'),
        ('Lake Mutanda', 'Uganda', 'Kisoro', 'A lake destination used for developing water and mountain-view experiences.', 'Water'),
        ('Sezibwa Falls', 'Uganda', 'Mukono', 'A waterfall destination used for developing nature and cultural experiences.', 'Water'),
        ('Bigodi Wetland Sanctuary', 'Uganda', 'Fort Portal', 'A wetland destination used for developing nature and community travel experiences.', 'Nature'),
        ('Karamoja Cultural Region', 'Uganda', 'Moroto', 'A regional destination used for developing respectful cultural discovery experiences.', 'Culture'),
        ('Ndere Cultural Centre', 'Uganda', 'Kampala', 'A cultural venue used for developing performing-arts and heritage experiences.', 'Culture')
)
INSERT INTO destinations (
    id, name, country, city, description, category,
    latitude, longitude, cover_image_url, created_at, updated_at
)
SELECT
    gen_random_uuid(), seed.name, seed.country, seed.city, seed.description,
    seed.category, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed
WHERE NOT EXISTS (
    SELECT 1
    FROM destinations existing
    WHERE LOWER(existing.name) = LOWER(seed.name)
      AND LOWER(existing.country) = LOWER(seed.country)
);

COMMIT;
