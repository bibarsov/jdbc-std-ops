### What for ###
The idea is to create crud-repository with rigid and predictable contract without ORM and JPA.   
Also if you ever think to migrate from hibernate repositories to plain jdbc repositories 
you'll probably find it useful.  
It mimics `findOne`, `findAll`, `save` and other operations in order to
make the transition easier.  
This library is supposed to work with Postgres database.

### Basic usage ###
1. Create entity with `@Table` annotation
2. Define `@Id` and `@Column` for every corresponding field
3. If your table uses sequence for id generation - declare `DeferredId<T>` field with `@DbSideId` annotation
4. Create instance of `StandardOperations` class inside your dao / repository
5. Enjoy

For more information and examples please take a look into test folder.
