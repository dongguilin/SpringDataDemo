package org.springframework.data.elasticsearch.repositories;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.Book;
import org.springframework.data.elasticsearch.repositories.book.SampleBookRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.*;

import static java.util.Arrays.asList;
import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/springContext-book-test.xml")
public class SampleBookRepositoryTest {

    @Resource
    private SampleBookRepository sampleBookRepository;

    @Resource
	private ElasticsearchTemplate template;

	@Before
    public void emptyData(){
        sampleBookRepository.deleteAll();
    }

    @Test
    public void shouldIndexSingleBookEntity(){

        Book book = new Book();
        book.setId("123455");
        book.setName("Spring Data Elasticsearch");
        book.setVersion(System.currentTimeMillis());
        sampleBookRepository.save(book);
        //lets try to search same record in elasticsearch
        Book indexedBook = sampleBookRepository.findOne(book.getId());
        assertThat(indexedBook,is(notNullValue()));
        assertThat(indexedBook.getId(),is(book.getId()));
    }

    @Test
    public void shouldBulkIndexMultipleBookEntities(){

        Book book1 = new Book(RandomStringUtils.random(5),"Spring Data",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),"Spring Data Elasticsearch",System.currentTimeMillis());
        //Bulk Index using sampleBookRepository
        sampleBookRepository.save(asList(book1, book2));
        //lets try to search same records in elasticsearch
        Book indexedBook1 = sampleBookRepository.findOne(book1.getId());
        assertThat(indexedBook1.getId(), is(book1.getId()));
        Book indexedBook2 = sampleBookRepository.findOne(book2.getId());
        assertThat(indexedBook2.getId(),is(book2.getId()));
    }

    @Test
    @Ignore("not to run as just for showing usage of sampleBookRepository ! might throw java.lang.OutOfMemoryError :-) ")
    public void crudRepositoryTest(){

        Book book1 = new Book(RandomStringUtils.random(5),"Spring Data",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),"Spring Data Elasticsearch",System.currentTimeMillis());
        List<Book> books = Arrays.asList(book1,book2);

        //indexing single document
        sampleBookRepository.save(book1);
        //bulk indexing multiple documents
        sampleBookRepository.save(books);
        //searching single document based on documentId
        Book book = sampleBookRepository.findOne(book1.getId());
        //to get all records as iteratable collection
        Iterable<Book> bookList = sampleBookRepository.findAll();
        //page request which will give first 10 document
        Page<Book> bookPage = sampleBookRepository.findAll(new PageRequest(0,10));
        // to get all records as ASC on name field
        Iterable<Book> bookIterable = sampleBookRepository.findAll(new Sort(new Sort.Order(Sort.Direction.ASC,"name")));
        //to get total number of docoments in an index
        Long count = sampleBookRepository.count();
        //to check wheather document exists or not
        boolean exists = sampleBookRepository.exists(book1.getId());
        //delete a document by entity
        sampleBookRepository.delete(book1);
        //delete multiple document using collection
        sampleBookRepository.delete(books);
        //delete a document using documentId
        sampleBookRepository.delete(book1.getId());
        //delete all document
        sampleBookRepository.deleteAll();
    }

    @Test
    public void shouldCountAllElementsInIndex(){

        List<Book> books = new ArrayList<Book>();
        for(int i=1; i<=10 ; i++){
           books.add(new Book(RandomStringUtils.random(5),"Spring Data Rocks !",System.currentTimeMillis()));

        }
        //Bulk Index using sampleBookRepository
        sampleBookRepository.save(books);
        //count all elements
        long count = sampleBookRepository.count();
        assertThat(count,is(equalTo(10L)));
    }

    @Test
    public void shouldExecuteCustomSearchQueries(){

        Book book1 = new Book(RandomStringUtils.random(5),"Custom Query",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),null,System.currentTimeMillis());
        //indexing a book
        sampleBookRepository.save(Arrays.asList(book1, book2));

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withFilter(boolFilter().must(existsFilter("name")))
                .withPageable(new PageRequest(0,10))
                .build();

        Page<Book> books = sampleBookRepository.search(searchQuery);
        assertThat(books.getNumberOfElements(), is(equalTo(1)));
    }

    /*@Test
    public void shouldExecuteCustomSearchQuery(){
        Book book1 = new Book(RandomStringUtils.random(5),"Custom Query",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),"Elasticsearch QueryBuilder",System.currentTimeMillis());
        //bulk indexing two documents
        sampleBookRepository.save(Arrays.asList(book1, book2));
        QueryBuilder queryBuilder = QueryBuilders.fieldQuery("name",book1.getName());
        //searching in elasticsearch using sampleBookRepository Page<E> search(QueryBuilder q, PageRequest p ) method.
        Page<Book> books =  sampleBookRepository.search(queryBuilder,new PageRequest(0,20));
        assertThat(books.getNumberOfElements(),is(equalTo(1)));
    }*/

    @Test
    public void shouldReturnBooksForCustomMethodsWithAndCriteria(){
        Book book1 = new Book(RandomStringUtils.random(5),"test",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),"test",System.currentTimeMillis());
        book1.setPrice(10L);
        book2.setPrice(10L);
        sampleBookRepository.save(Arrays.asList(book1, book2));

        Page<Book> books = sampleBookRepository.findByNameAndPrice("test", 10, new PageRequest(0, 10));
        assertThat(books.getContent().size(), is(2));
    }

    @Test
    public void shouldReturnBooksWithName(){
        Book book1 = new Book(RandomStringUtils.random(5),"test1",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),"test2",System.currentTimeMillis());
        sampleBookRepository.save(Arrays.asList(book1, book2));

        Page<Book> books = sampleBookRepository.findByName("test1", new PageRequest(0, 10));
        assertThat(books.getContent().size(), is(1));
    }

	@Test
	public void shouldReturnBooksForGivenBucket(){
		Book book1 = new Book(RandomStringUtils.random(5),"test1",System.currentTimeMillis());
		Book book2 = new Book(RandomStringUtils.random(5),"test2",System.currentTimeMillis());

		Map<Integer, Collection<String>> map1 = new HashMap<Integer, Collection<String>>();
		map1.put(1, Arrays.asList("test1", "test2"));

		Map<Integer, Collection<String>> map2 = new HashMap<Integer, Collection<String>>();
		map2.put(1, Arrays.asList("test3", "test4"));

		book1.setBuckets(map1);
		book2.setBuckets(map2);

		sampleBookRepository.save(Arrays.asList(book1,book2));

		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(nestedQuery("buckets", termQuery("buckets.1", "test3")))
				.build();

		Page<Book> books = sampleBookRepository.search(searchQuery);

		assertThat(books.getContent().size(), is(1));
	}


	@Test
	public void shouldReturnBooksForGivenBucketUsingTemplate(){

		template.deleteIndex(Book.class);
		template.createIndex(Book.class);
		template.putMapping(Book.class);
		template.refresh(Book.class, true);

		Book book1 = new Book(RandomStringUtils.random(5),"test1",System.currentTimeMillis());
		Book book2 = new Book(RandomStringUtils.random(5),"test2",System.currentTimeMillis());

		Map<Integer, Collection<String>> map1 = new HashMap<Integer, Collection<String>>();
		map1.put(1, Arrays.asList("test1", "test2"));

		Map<Integer, Collection<String>> map2 = new HashMap<Integer, Collection<String>>();
		map2.put(1, Arrays.asList("test3", "test4"));

		book1.setBuckets(map1);
		book2.setBuckets(map2);

		sampleBookRepository.save(Arrays.asList(book1,book2));

		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(nestedQuery("buckets", termQuery("buckets.1", "test3")))
				.build();

		Page<Book> books = sampleBookRepository.search(searchQuery);

		assertThat(books.getContent().size(), is(1));
	}

	//    //todo
    @Ignore
    @Test
    public void shouldReturnBooksForCustomMethodsWithOrCriteria(){
        Book book1 = new Book(RandomStringUtils.random(5),"test Or",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),"test And",System.currentTimeMillis());
        book1.setPrice(10L);
        book2.setPrice(10L);
        sampleBookRepository.save(Arrays.asList(book1, book2));

        Page<Book> books = sampleBookRepository.findByNameOrPrice("message", 10, new PageRequest(0, 10));
        assertThat(books.getContent().size(), is(2));
    }

    @Test
    public void shouldGiveIterableOfBooks() {
        Book book1 = new Book(RandomStringUtils.random(5),"test Or",System.currentTimeMillis());
        Book book2 = new Book(RandomStringUtils.random(5),"test And",System.currentTimeMillis());
        book1.setPrice(10L);
        book2.setPrice(10L);
        sampleBookRepository.save(Arrays.asList(book1, book2));

        Iterable<Book> books = sampleBookRepository.search(matchAllQuery());
        assertThat(books, is(notNullValue()));
    }

}
