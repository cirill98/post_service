package faang.school.postservice.filter.album;

import faang.school.postservice.dto.album.AlbumFilterDto;
import faang.school.postservice.model.Album;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class AlbumAuthorFilter implements AlbumFilter {
    @Override
    public boolean isApplicable(AlbumFilterDto albumFilterDto) {
        return albumFilterDto.getAuthorId() != null;
    }

    @Override
    public Stream<Album> apply(Stream<Album> albums, AlbumFilterDto albumFilterDto) {
        return albums.filter(album ->
                albumFilterDto.getAuthorId().equals(album.getAuthorId()));
    }
}