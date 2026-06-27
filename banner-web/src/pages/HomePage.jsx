import React from 'react';
import SearchBar from '../components/SearchBar'; 

const HomePage = () => {
  return (
    <div className="home-page" style={{ height: '100%', display: 'flex', alignItems: 'center' }}>
      <SearchBar placeholder="Tìm kiếm" />
    </div>
  );
};

export default HomePage;
