package pt.raidline.api.fuzzy.custom;

import pt.raidline.api.fuzzy.processors.paths.model.Path;

import java.util.Iterator;
import java.util.List;

//No reason to believe at this point, we will have anything else as type

//This is basically an infinite iterator, if we wanted to.
// The number of cycles should be passed as a param
public class PathSupplierIterator implements Iterator<Iterator<Path>> {

    private final List<Path> upstream;
    private final int numberOfCycles;
    private int count;

    public PathSupplierIterator(List<Path> upstream, int numberOfCycles) {
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
            var value = items.get(count);
            count++;
            return value;
        }
    }
}
