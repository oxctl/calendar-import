package uk.ac.ox.it.calendarimporter;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * This class is just used so that when limiting the attribues Jackson serialises it includes the
 * paging information.
 *
 * @param <T> The type of objects in the page.
 */
public class JsonPage<T> extends PageImpl<T> {

    public JsonPage(final List<T> content, final Pageable pageable, final long total) {
        super(content, pageable, total);
    }

    public JsonPage(final List<T> content) {
        super(content);
    }

    public JsonPage(final Page<T> page, final Pageable pageable) {
        super(page.getContent(), pageable, page.getTotalElements());
    }

    @JsonView(Views.Public.class)
    public int getTotalPages() {
        return super.getTotalPages();
    }

    @JsonView(Views.Public.class)
    public long getTotalElements() {
        return super.getTotalElements();
    }

    @JsonView(Views.Public.class)
    public boolean hasNext() {
        return super.hasNext();
    }

    @JsonView(Views.Public.class)
    public boolean isLast() {
        return super.isLast();
    }

    @JsonView(Views.Public.class)
    public boolean hasContent() {
        return super.hasContent();
    }

    @JsonView(Views.Public.class)
    public List<T> getContent() {
        return super.getContent();
    }
}
