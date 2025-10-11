-- Create Database
CREATE DATABASE video_browsing_system;
USE video_browsing_system;

-- Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    bio TEXT,
    profile_picture VARCHAR(255),
    role ENUM('ADMIN', 'CONTENT_CREATOR', 'REGISTERED_USER') DEFAULT 'REGISTERED_USER',
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    terms_agreed BOOLEAN DEFAULT FALSE,
    privacy_settings JSON,
    notification_settings JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Categories Table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Videos Table
CREATE TABLE videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    duration INT, -- in seconds
    file_size BIGINT, -- in bytes
    resolution VARCHAR(20), -- e.g., "1080p", "720p"
    category_id BIGINT,
    uploaded_by BIGINT NOT NULL,
    privacy ENUM('PUBLIC', 'PRIVATE') DEFAULT 'PUBLIC',
    status ENUM('PUBLISHED', 'DISABLED', 'PROCESSING') DEFAULT 'PROCESSING',
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    dislike_count BIGINT DEFAULT 0,
    tags JSON, -- Store tags as JSON array
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_title (title),
    INDEX idx_category (category_id),
    INDEX idx_uploader (uploaded_by),
    INDEX idx_status (status),
    INDEX idx_privacy (privacy)
);

-- Comments Table
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_spam BOOLEAN DEFAULT FALSE,
    is_disabled BOOLEAN DEFAULT FALSE,
    parent_comment_id BIGINT, -- For reply functionality
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_video (video_id),
    INDEX idx_user (user_id)
);

-- Comment Likes Table
CREATE TABLE comment_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_comment_like (comment_id, user_id),
    INDEX idx_comment_like_comment (comment_id),
    INDEX idx_comment_like_user (user_id)
);

-- Ratings Table
CREATE TABLE ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating_type ENUM('LIKE', 'DISLIKE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_video_rating (user_id, video_id),
    INDEX idx_video_rating (video_id),
    INDEX idx_user_rating (user_id)
);

-- Star Ratings Table (1-5 scores)
CREATE TABLE video_user_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score TINYINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_video_user_rating (video_id, user_id),
    INDEX idx_video_user_rating_video (video_id),
    INDEX idx_video_user_rating_user (user_id)
);

-- Subscriptions Table
CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subscriber_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_subscriptions_subscriber_creator (subscriber_id, creator_id),
    INDEX idx_subscriptions_creator (creator_id),
    INDEX idx_subscriptions_subscriber (subscriber_id)
);

-- Playlists Table
CREATE TABLE playlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id BIGINT NOT NULL,
    privacy ENUM('PUBLIC', 'PRIVATE') DEFAULT 'PRIVATE',
    is_collaborative BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_playlist (user_id)
);

-- Playlist Videos Table (Many-to-Many relationship)
CREATE TABLE playlist_videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    position INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    UNIQUE KEY unique_playlist_video (playlist_id, video_id),
    INDEX idx_playlist (playlist_id),
    INDEX idx_video_playlist (video_id)
);

-- Watch Later Table
CREATE TABLE watch_later (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_video_watchlater (user_id, video_id),
    INDEX idx_user_watchlater (user_id)
);

-- Reports Table
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT,
    comment_id BIGINT,
    reported_by BIGINT NOT NULL,
    report_type ENUM('SPAM', 'INAPPROPRIATE_CONTENT', 'COPYRIGHT', 'PLAYBACK_ISSUE', 'OTHER') NOT NULL,
    description TEXT,
    status ENUM('PENDING', 'REVIEWED', 'RESOLVED', 'DISMISSED') DEFAULT 'PENDING',
    admin_notes TEXT,
    resolved_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_reported_by (reported_by),
    INDEX idx_status (status),
    INDEX idx_type (report_type)
);

-- Video Views Table (for analytics)
CREATE TABLE video_views (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT, -- NULL for guest users
    ip_address VARCHAR(45),
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_video_views (video_id),
    INDEX idx_user_views (user_id),
    INDEX idx_viewed_at (viewed_at)
);

-- Insert sample categories
INSERT INTO categories (name, description) VALUES 
('Entertainment', 'Entertainment videos including movies, music, and comedy'),
('Education', 'Educational content including tutorials and lectures'),
('Technology', 'Technology related videos'),
('Sports', 'Sports and fitness videos'),
('Gaming', 'Gaming videos and live streams'),
('News', 'News and current affairs');

-- Insert admin user (password: admin123)
INSERT INTO users (username, email, password, first_name, last_name, role, email_verified, terms_agreed) VALUES 
('admin', 'admin@videosystem.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'System', 'Administrator', 'ADMIN', TRUE, TRUE);