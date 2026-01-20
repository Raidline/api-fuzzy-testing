package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.processors.paths.model.Path;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

//No reason to believe at this point, we will have anything else as type

//This is basically an infinite iterator, if we wanted to.
// The number of cycles should be passed as a param
class PathSupplierIterator implements Iterator<Iterator<Path>> {

    private final List<Path> upstream;
    private final int numberOfCycles;
    private int count;

    PathSupplierIterator(List<Path> upstream, int numberOfCycles) {
        this.upstream = upstream;
        this.numberOfCycles = numberOfCycles;
        this.count = 0;
    }

    @Override
    public boolean hasNext() {
        return count < numberOfCycles;
    }

    @Override
    public Iterator<Path> next() {
        if (count >= numberOfCycles) {
            throw new NoSuchElementException();
        }
        count++;
        return new InternalIterator(this.upstream);
    }

    private static class InternalIterator implements Iterator<Path> {
        private final List<Path> items;
        private int count;

        private InternalIterator(List<Path> items) {
            this.items = items;
            this.count = 0;
        }

        @Override
        public boolean hasNext() {
            return count < items.size();
        }

        @Override
        public Path next() {
            if (count >= items.size()) {
                throw new NoSuchElementException();
            }
            var value = items.get(count);
            count++;
            return value;
        }
    }
}
