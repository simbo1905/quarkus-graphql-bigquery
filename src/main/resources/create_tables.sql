 create table demo_graphql_java.book ( id string, name string, pageCount string, authorId string );   
 insert into demo_graphql_java.book (  id,  name,  pageCount,  authorId)  VALUES ( 'book-1',  'Harry Potter and the Philosopher''s Stone', '223', 'author-1') ;  
 insert into demo_graphql_java.book (  id,  name,  pageCount,  authorId)  VALUES (  'book-2', 'Moby Dick', '635', 'author-2' );  
 insert into demo_graphql_java.book (  id,  name,  pageCount,  authorId)  VALUES (  'book-3', 'Interview with the vampire', '371', 'author-3' );  
 
 create table demo_graphql_java.author ( id string, firstName string, lastName string );  
 insert into demo_graphql_java.author  ( id, firstName, lastName ) VALUES ( 'author-1', 'Joanne', 'Rowling' );  
 insert into demo_graphql_java.author  ( id, firstName, lastName ) VALUES ( 'author-2', 'Herman', 'Melville' );  
 insert into demo_graphql_java.author  ( id, firstName, lastName ) VALUES ( 'author-3', 'Anne', 'Rice' );
 