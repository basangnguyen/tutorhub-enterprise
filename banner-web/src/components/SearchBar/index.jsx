import React, { useState, useEffect } from 'react';
import './SearchBar.css';
import { getDailyImage } from '../../services/imageProviderService';

const SearchBar = ({ placeholder }) => {
  const [query, setQuery] = useState('');
  const [dailyImage, setDailyImage] = useState(null);

  useEffect(() => {
    // Fetch daily image for the thumbnail
    getDailyImage({ preferSource: 'curated' })
      .then(img => {
        if (img) setDailyImage(img);
      })
      .catch(err => console.error("Failed to load thumbnail:", err));
  }, []);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      if (window.javaInterface && typeof window.javaInterface.search === 'function') {
        window.javaInterface.search(query);
      } else {
        console.log("Tìm kiếm:", query);
      }
    }
  };

  const handleSearchBtnClick = () => {
    if (window.javaInterface && typeof window.javaInterface.search === 'function') {
      window.javaInterface.search(query);
    }
  };

  return (
    <div className="search-bar-container">
      <button className="search-icon-btn" onClick={handleSearchBtnClick}>
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="search-icon">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
      </button>

      <input 
        type="text" 
        className="search-input" 
        placeholder={placeholder} 
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={handleKeyDown}
      />

      {dailyImage && (
        <button 
          className="search-thumbnail-btn" 
          title={dailyImage.name_vi + (dailyImage.location_vi ? ' - ' + dailyImage.location_vi : '')}
          onClick={() => {
            if (dailyImage.wikipedia_url && window.javaInterface && typeof window.javaInterface.openUrl === 'function') {
              window.javaInterface.openUrl(dailyImage.wikipedia_url);
            } else if (dailyImage.wikipedia_url) {
              window.open(dailyImage.wikipedia_url, '_blank');
            }
          }}
        >
          <img src={dailyImage.image_thumb || dailyImage.image_url} alt={dailyImage.name_vi} />
          {dailyImage.region && (
            <span className="search-thumbnail-badge">VN</span>
          )}
        </button>
      )}
    </div>
  );
};

export default SearchBar;
